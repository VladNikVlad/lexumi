package com.lexumi.app.presentation.learnwords

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.data.datastore.UserPreferences
import com.lexumi.app.domain.model.AnswerCheck
import com.lexumi.app.domain.model.Rule
import com.lexumi.app.domain.model.Word
import com.lexumi.app.domain.repository.LanguageRepository
import com.lexumi.app.domain.repository.RuleRepository
import com.lexumi.app.domain.repository.SectionRepository
import com.lexumi.app.domain.repository.TopicRepository
import com.lexumi.app.domain.repository.WordRepository
import com.lexumi.app.domain.usecase.BuildMultipleChoiceUseCase
import com.lexumi.app.domain.usecase.GetCardsRoundWordsUseCase
import com.lexumi.app.domain.usecase.GetSessionWordsUseCase
import com.lexumi.app.domain.usecase.SubmitWordAnswerUseCase
import com.lexumi.app.domain.usecase.TranslationParser
import com.lexumi.app.util.SoundFeedbackPlayer
import com.lexumi.app.util.TtsManager
import com.lexumi.app.util.VoiceRecognizerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Which of the 3 inline (main-pass) practice formats a word rating maps to. */
enum class WordPromptMode { CHOICE, TYPED, HEAR_ONLY }

data class WordPrompt(
    val word: Word,
    val mode: WordPromptMode,
    /** What's shown on the card — null for HEAR_ONLY, where nothing is shown, only heard. */
    val displayText: String?,
    /** What the auto-read-aloud / replay button says. */
    val speakText: String,
    /** What a typed (or spoken) answer is checked against. Unused for CHOICE. */
    val expectedAnswer: String,
    val choices: List<String>? = null,
)

sealed class WordFeedback {
    object None : WordFeedback()
    object Correct : WordFeedback()
    data class OneLetterTypo(val correctSpelling: String) : WordFeedback()
    data class Wrong(val correctSpelling: String) : WordFeedback()
}

data class MatchingPair(val wordId: Long, val term: String, val translation: String)

data class MatchingGameState(
    val pairs: List<MatchingPair>,
    val leftOrder: List<Long>,
    val rightOrder: List<Long>,
    val matchedIds: Set<Long> = emptySet(),
    val selectedLeftId: Long? = null,
    val selectedRightId: Long? = null,
    val wrongFlashLeftId: Long? = null,
    val wrongFlashRightId: Long? = null,
)

data class VoiceCard(val wordId: Long, val term: String, val translation: String)

data class VoiceMasteryState(
    val cards: List<VoiceCard>,
    val index: Int = 0,
    val listening: Boolean = false,
    /** What the recognizer actually heard, shown for feedback once a card is answered. */
    val heard: String? = null,
    /** null while listening/unanswered; true/false once a card has been checked. */
    val correct: Boolean? = null,
    /** Live status/partial-hypothesis/error text from the recognizer — for debugging why
     * recognition isn't picking anything up. Shown under the card at all times while testing. */
    val debug: String? = null,
)

data class LearnWordsUiState(
    val loading: Boolean = true,
    val prompt: WordPrompt? = null,
    val feedback: WordFeedback = WordFeedback.None,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val sessionDone: Boolean = false,
    val editError: String? = null,
    /** True once the main pass is done and we're re-testing only the words that were ever wrong this session. */
    val inMistakeReview: Boolean = false,
    /** "Кажи вголос" cards round for rating-2 words — happens once, before the pair-matching round. */
    val voiceMastery: VoiceMasteryState? = null,
    /** "Знайти пару" round for the rating 0/1 words practiced this session — happens last. */
    val matchingGame: MatchingGameState? = null,
    /** True once the user has tapped "Я зараз не можу говорити" — no more voice-required tasks this session. */
    val voiceDisabled: Boolean = false,
    /** Live status/partial/error text from the recognizer for the rating-3 mic option — for debugging. */
    val voiceDebug: String? = null,
)

@HiltViewModel
class LearnWordsViewModel @Inject constructor(
    private val getSessionWords: GetSessionWordsUseCase,
    private val getCardsRoundWords: GetCardsRoundWordsUseCase,
    private val buildMultipleChoice: BuildMultipleChoiceUseCase,
    private val submitAnswer: SubmitWordAnswerUseCase,
    private val editWord: com.lexumi.app.domain.usecase.EditWordUseCase,
    private val deleteWord: com.lexumi.app.domain.usecase.DeleteWordUseCase,
    private val wordRepository: WordRepository,
    private val topicRepository: TopicRepository,
    private val sectionRepository: SectionRepository,
    private val languageRepository: LanguageRepository,
    ruleRepository: RuleRepository,
    private val prefs: UserPreferences,
    private val ttsManager: TtsManager,
    private val voiceRecognizer: VoiceRecognizerManager,
    private val soundFeedbackPlayer: SoundFeedbackPlayer,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val topicId: Long = checkNotNull(savedStateHandle["topicId"])

    private val _uiState = MutableStateFlow(LearnWordsUiState())
    val uiState: StateFlow<LearnWordsUiState> = _uiState

    private val _languageId = MutableStateFlow<Long?>(null)
    val rules: StateFlow<List<Rule>> = _languageId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else ruleRepository.observeRules(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** The language's auto-assigned TTS voice, so words are read aloud in a natural voice for that language. */
    private val _voiceName = MutableStateFlow<String?>(null)
    val voiceName: StateFlow<String?> = _voiceName

    private var queue: MutableList<Long> = mutableListOf()
    private var distinctWordIds: List<Long> = emptyList()

    // --- session-only mistake tracking: retry a word within its own scheduled
    // repeats, then a single "work on mistakes" pass once the main pass is done ---
    private val everWrongIds = mutableSetOf<Long>()
    private var mistakeReviewStarted = false
    private var cardsRoundStarted = false
    private var matchingGameStarted = false

    // "Я зараз не можу говорити" — once pressed, no more voice-required tasks this session.
    private var voiceDisabledThisSession = false

    init {
        viewModelScope.launch {
            prefs.setLastSession(topicId, "learn_words")
            val wordsPerSession = prefs.wordsPerSession.first()
            val repetitions = prefs.repetitions.first()
            queue = getSessionWords(topicId, wordsPerSession, repetitions).toMutableList()
            distinctWordIds = queue.distinct()
            _uiState.value = _uiState.value.copy(totalCount = queue.size, loading = false)
            advance()

            val topic = topicRepository.getTopic(topicId)
            val languageId = topic?.let { sectionRepository.getSection(it.sectionId)?.languageId }
            _languageId.value = languageId
            _voiceName.value = languageId?.let { languageRepository.getLanguage(it)?.voiceName }
        }
    }

    private suspend fun advance() {
        if (queue.isEmpty()) {
            if (!mistakeReviewStarted && everWrongIds.isNotEmpty()) {
                mistakeReviewStarted = true
                queue = everWrongIds.toMutableList().apply { shuffle() }
                _uiState.value = _uiState.value.copy(inMistakeReview = true)
            } else if (!cardsRoundStarted) {
                cardsRoundStarted = true
                enterCardsRound()
                return
            } else if (!matchingGameStarted) {
                matchingGameStarted = true
                enterMatchingGame()
                return
            } else {
                finishSession()
                return
            }
        }
        // Re-fetch the word fresh each time: an earlier repeat in this same
        // session may have changed its rating, and the prompt should reflect
        // that current state, not a stale snapshot from the start.
        val wordId = queue.removeAt(0)
        val word = wordRepository.getWord(wordId) ?: run { advance(); return }
        val prompt = buildPrompt(word) ?: run { advance(); return } // rating 2/4 shouldn't be in the main queue at all
        _uiState.value = _uiState.value.copy(prompt = prompt, feedback = WordFeedback.None)
    }

    private suspend fun buildPrompt(word: Word): WordPrompt? = when (word.rating) {
        0 -> WordPrompt(
            word = word, mode = WordPromptMode.CHOICE,
            displayText = word.term, speakText = word.term, expectedAnswer = word.translation,
            choices = buildMultipleChoice(word),
        )
        1 -> if (!word.typedReverseActive) {
            WordPrompt(word, WordPromptMode.TYPED, word.term, word.term, word.translation)
        } else {
            WordPrompt(word, WordPromptMode.TYPED, word.translation, word.translation, word.term)
        }
        3 -> WordPrompt(word, WordPromptMode.HEAR_ONLY, null, word.term, word.translation)
        else -> null
    }

    private suspend fun finishSession() {
        prefs.clearLastSession()
        _uiState.value = _uiState.value.copy(sessionDone = true, prompt = null, voiceMastery = null, matchingGame = null)
    }

    // ---------------- cards round (rating 2, voice) ----------------

    /** All of the topic's rating-2 words — not just the ones seen this session, since progress
     * toward rating 3 is tracked on the word itself and carries over between sessions. */
    private suspend fun enterCardsRound() {
        val words = if (voiceDisabledThisSession) emptyList() else getCardsRoundWords(topicId)
        if (words.isEmpty()) { advance(); return }
        _uiState.value = _uiState.value.copy(
            prompt = null,
            inMistakeReview = false,
            voiceMastery = VoiceMasteryState(cards = words.map { VoiceCard(it.id, it.term, it.translation) }),
        )
    }

    /** Starts listening for the current voice card; stops itself after ~2s of silence. */
    fun startListeningForCurrentVoiceCard() {
        val state = _uiState.value.voiceMastery ?: return
        val card = state.cards.getOrNull(state.index) ?: return
        _uiState.value = _uiState.value.copy(voiceMastery = state.copy(listening = true, heard = null, correct = null, debug = null))
        val locale = ttsManager.localeFor(_voiceName.value)
        voiceRecognizer.listenOnce(
            locale = locale,
            onPartial = { partial ->
                val current = _uiState.value.voiceMastery ?: return@listenOnce
                _uiState.value = _uiState.value.copy(voiceMastery = current.copy(debug = "Чую: «$partial»"))
            },
            onDebug = { line ->
                val current = _uiState.value.voiceMastery ?: return@listenOnce
                _uiState.value = _uiState.value.copy(voiceMastery = current.copy(debug = line))
            },
            onResult = { heardRaw ->
                // Checks the full phrase first, then trailing windows of increasing size, so a
                // multi-word answer (e.g. "la pizarra") still matches even if the recognizer
                // tacked on extra mis-heard words.
                val expected = TranslationParser.acceptableAnswers(card.term)
                val isCorrect = voiceRecognizer.matches(heardRaw, expected)
                if (isCorrect) soundFeedbackPlayer.playCorrect() else soundFeedbackPlayer.playWrong()
                viewModelScope.launch {
                    val word = wordRepository.getWord(card.wordId)
                    if (word != null) submitAnswer.submitVoiceCardAnswer(word, isCorrect)
                    val current = _uiState.value.voiceMastery ?: return@launch
                    _uiState.value = _uiState.value.copy(voiceMastery = current.copy(listening = false, heard = heardRaw, correct = isCorrect))
                }
            },
        )
    }

    fun nextVoiceCard() {
        val state = _uiState.value.voiceMastery ?: return
        val nextIndex = state.index + 1
        if (nextIndex >= state.cards.size) {
            viewModelScope.launch { advance() }
        } else {
            _uiState.value = _uiState.value.copy(voiceMastery = state.copy(index = nextIndex, listening = false, heard = null, correct = null))
        }
    }

    /** "Вже знаю" during the cards round: this rating-2 word jumps straight to mastered (4). */
    fun markCurrentVoiceCardAsKnown() {
        val state = _uiState.value.voiceMastery ?: return
        val card = state.cards.getOrNull(state.index) ?: return
        viewModelScope.launch {
            val word = wordRepository.getWord(card.wordId)
            if (word != null) submitAnswer.markAsKnown(word)
            nextVoiceCard()
        }
    }

    /** "Я зараз не можу говорити" — ends the cards round for this session (progress untouched,
     * these words simply come back next session) and moves straight to matching pairs. */
    fun voiceUnavailable() {
        voiceDisabledThisSession = true
        voiceRecognizer.stop()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(voiceMastery = null, voiceDisabled = true)
            advance()
        }
    }

    // ---------------- "Знайти пару" round (rating 0/1, this session's words) ----------------

    private suspend fun enterMatchingGame() {
        // No rating filter here — this reviews whatever words were actually practiced in this
        // session's main pass, regardless of what rating they ended on (a word promoted to the
        // cards round mid-session shouldn't vanish from its own session's matching game).
        val words = distinctWordIds.mapNotNull { wordRepository.getWord(it) }
        if (words.isEmpty()) { finishSession(); return }
        val pairs = words.map { MatchingPair(it.id, it.term, it.translation) }
        _uiState.value = _uiState.value.copy(
            prompt = null,
            inMistakeReview = false,
            matchingGame = MatchingGameState(
                pairs = pairs,
                leftOrder = pairs.map { it.wordId }.shuffled(),
                rightOrder = pairs.map { it.wordId }.shuffled(),
            ),
        )
    }

    fun selectMatchingLeft(wordId: Long) {
        val game = _uiState.value.matchingGame ?: return
        if (wordId in game.matchedIds) return
        if (game.selectedLeftId == wordId) {
            _uiState.value = _uiState.value.copy(matchingGame = game.copy(selectedLeftId = null, wrongFlashLeftId = null, wrongFlashRightId = null))
            return
        }
        val updated = game.copy(selectedLeftId = wordId, wrongFlashLeftId = null, wrongFlashRightId = null)
        _uiState.value = _uiState.value.copy(matchingGame = updated)
        tryMatch(updated)
    }

    fun selectMatchingRight(wordId: Long) {
        val game = _uiState.value.matchingGame ?: return
        if (wordId in game.matchedIds) return
        if (game.selectedRightId == wordId) {
            _uiState.value = _uiState.value.copy(matchingGame = game.copy(selectedRightId = null, wrongFlashLeftId = null, wrongFlashRightId = null))
            return
        }
        val updated = game.copy(selectedRightId = wordId, wrongFlashLeftId = null, wrongFlashRightId = null)
        _uiState.value = _uiState.value.copy(matchingGame = updated)
        tryMatch(updated)
    }

    private fun tryMatch(game: MatchingGameState) {
        val left = game.selectedLeftId
        val right = game.selectedRightId
        if (left == null || right == null) return
        if (left == right) {
            val matched = game.matchedIds + left
            _uiState.value = _uiState.value.copy(matchingGame = game.copy(matchedIds = matched, selectedLeftId = null, selectedRightId = null))
            if (matched.size == game.pairs.size) {
                viewModelScope.launch { finishSession() }
            }
        } else {
            _uiState.value = _uiState.value.copy(
                matchingGame = game.copy(selectedLeftId = null, selectedRightId = null, wrongFlashLeftId = left, wrongFlashRightId = right),
            )
        }
    }

    // ---------------- main-pass answers ----------------

    private fun registerMistake(wordId: Long) { everWrongIds.add(wordId) }
    private fun registerSuccess(wordId: Long) { /* still covered by the single end-of-session review */ }

    fun submitChoice(chosenText: String) {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch {
            val wasCorrect = chosenText == TranslationParser.displayPrimary(prompt.expectedAnswer)
            submitAnswer.submitChoice(prompt.word, wasCorrect)
            if (wasCorrect) { soundFeedbackPlayer.playCorrect(); registerSuccess(prompt.word.id) }
            else { soundFeedbackPlayer.playWrong(); registerMistake(prompt.word.id) }
            _uiState.value = _uiState.value.copy(
                feedback = if (wasCorrect) WordFeedback.Correct else WordFeedback.Wrong(prompt.expectedAnswer.trim()),
                completedCount = _uiState.value.completedCount + 1,
            )
        }
    }

    fun submitTyped(userInput: String) {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch {
            val (_, check) = submitAnswer.submitTypedAnswer(prompt.word, userInput, prompt.expectedAnswer)
            if (check is AnswerCheck.Wrong) { soundFeedbackPlayer.playWrong(); registerMistake(prompt.word.id) }
            else { soundFeedbackPlayer.playCorrect(); registerSuccess(prompt.word.id) }
            val feedback = when (check) {
                is AnswerCheck.Correct -> WordFeedback.Correct
                is AnswerCheck.OneLetterTypo -> WordFeedback.OneLetterTypo(check.correctSpelling)
                is AnswerCheck.Wrong -> WordFeedback.Wrong(check.correctSpelling)
            }
            _uiState.value = _uiState.value.copy(feedback = feedback, completedCount = _uiState.value.completedCount + 1)
        }
    }

    /** The hear-only (rating 3) mic option: spoken answer checked the same way as a typed one. */
    fun startListeningForHearOnlyAnswer() {
        val prompt = _uiState.value.prompt ?: return
        if (prompt.mode != WordPromptMode.HEAR_ONLY || voiceDisabledThisSession) return
        _uiState.value = _uiState.value.copy(voiceDebug = null)
        val nativeLocale = java.util.Locale.getDefault()
        voiceRecognizer.listenOnce(
            locale = nativeLocale,
            onPartial = { partial -> _uiState.value = _uiState.value.copy(voiceDebug = "Чую: «$partial»") },
            onDebug = { line -> _uiState.value = _uiState.value.copy(voiceDebug = line) },
            onResult = { heardRaw -> submitTyped(heardRaw) },
        )
    }

    /** "Я зараз не можу говорити" from within a hear-only prompt — just hides the mic option;
     * the word still has its typed fallback, so the main pass isn't interrupted. */
    fun disableVoiceForSession() {
        voiceDisabledThisSession = true
        voiceRecognizer.stop()
        _uiState.value = _uiState.value.copy(voiceDisabled = true)
    }

    /** "Вже знаю": ratings 0-1 jump to the cards round (2); ratings 2-3 jump to mastered (4). */
    fun markCurrentAsKnown() {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch {
            submitAnswer.markAsKnown(prompt.word)
            queue.removeAll { it == prompt.word.id }
            everWrongIds.remove(prompt.word.id)
            advance()
        }
    }

    fun addCurrentToReview() {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch { submitAnswer.toggleReviewList(prompt.word, true) }
    }

    fun editCurrentWord(term: String, translation: String, imagePath: String?, ruleId: Long?) {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch {
            when (val result = editWord(prompt.word, term, translation, imagePath, ruleId)) {
                is com.lexumi.app.domain.usecase.AddResult.Success -> {
                    val updated = prompt.word.copy(term = term.trim(), translation = translation.trim(), imagePath = imagePath, ruleId = ruleId)
                    _uiState.value = _uiState.value.copy(prompt = buildPrompt(updated) ?: prompt, editError = null)
                }
                com.lexumi.app.domain.usecase.AddResult.AlreadyExists -> _uiState.value = _uiState.value.copy(editError = "Таке слово вже є в цій темі")
                com.lexumi.app.domain.usecase.AddResult.Blank -> _uiState.value = _uiState.value.copy(editError = "Заповніть слово і переклад")
            }
        }
    }

    fun deleteCurrentWord() {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch {
            queue.removeAll { it == prompt.word.id }
            everWrongIds.remove(prompt.word.id)
            deleteWord(prompt.word)
            advance()
        }
    }

    fun clearEditError() { _uiState.value = _uiState.value.copy(editError = null) }

    fun speak(text: String) { ttsManager.speak(text, _voiceName.value) }

    /** Speaks native-language (Ukrainian) text with a Ukrainian voice, regardless of the topic's
     * own language — used for the translation side of the "Знайти пару" matching game. */
    fun speakNative(text: String) { ttsManager.speak(text, java.util.Locale("uk")) }

    fun next() {
        viewModelScope.launch { advance() }
    }

    override fun onCleared() {
        voiceRecognizer.stop()
        super.onCleared()
    }
}
