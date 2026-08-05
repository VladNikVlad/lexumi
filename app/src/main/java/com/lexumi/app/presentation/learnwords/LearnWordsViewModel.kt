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
import com.lexumi.app.domain.usecase.*
import com.lexumi.app.util.TtsManager
import com.lexumi.app.util.SoundFeedbackPlayer
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

data class WordPrompt(
    val word: Word,
    val askTermFirst: Boolean,
    val choices: List<String>?, // null when the word is at level 1 (typed answer)
)

sealed class WordFeedback {
    data object None : WordFeedback()
    data object Correct : WordFeedback()
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
    /** The "find the pair" bonus round shown once at the very end of the session. */
    val matchingGame: MatchingGameState? = null,
)

@HiltViewModel
class LearnWordsViewModel @Inject constructor(
    private val getSessionWords: GetSessionWordsUseCase,
    private val buildMultipleChoice: BuildMultipleChoiceUseCase,
    private val submitAnswer: SubmitWordAnswerUseCase,
    private val editWord: EditWordUseCase,
    private val deleteWord: DeleteWordUseCase,
    private val wordRepository: WordRepository,
    private val topicRepository: TopicRepository,
    private val sectionRepository: SectionRepository,
    private val languageRepository: LanguageRepository,
    ruleRepository: RuleRepository,
    private val prefs: UserPreferences,
    private val ttsManager: TtsManager,
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

    // --- session-only mistake tracking (point: retry a word until correct after 2 misses,
    // then a final "work on mistakes" pass once the whole session is otherwise done) ---
    private val wrongCounts = mutableMapOf<Long, Int>()
    private val everWrongIds = mutableSetOf<Long>()
    private var mistakeReviewStarted = false
    private var matchingGameStarted = false

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
                // Main pass is over, but some words were wrong at some point —
                // do one more "work on mistakes" round over just those.
                mistakeReviewStarted = true
                queue = everWrongIds.toMutableList().apply { shuffle() }
                _uiState.value = _uiState.value.copy(inMistakeReview = true)
            } else if (!matchingGameStarted) {
                matchingGameStarted = true
                enterMatchingGame()
                return
            } else {
                prefs.clearLastSession()
                _uiState.value = _uiState.value.copy(sessionDone = true, prompt = null)
                return
            }
        }
        // Re-fetch the word fresh each time: an earlier repeat in this same
        // session may have changed its level/score, and the prompt should
        // reflect that current state, not a stale snapshot from the start.
        val wordId = queue.removeAt(0)
        val word = wordRepository.getWord(wordId) ?: run { advance(); return }
        val askTermFirst = word.askTermFirst()
        val choices = if (word.level == 0) buildMultipleChoice(word, askTermFirst) else null
        _uiState.value = _uiState.value.copy(
            prompt = WordPrompt(word, askTermFirst, choices),
            feedback = WordFeedback.None,
        )
    }

    /** "Знайти пару" bonus round at the very end: the session's distinct words, term vs. translation. */
    private suspend fun enterMatchingGame() {
        val words = distinctWordIds.mapNotNull { wordRepository.getWord(it) }
        if (words.isEmpty()) {
            prefs.clearLastSession()
            _uiState.value = _uiState.value.copy(sessionDone = true, prompt = null, inMistakeReview = false)
            return
        }
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
        // Tapping the already-selected tile again cancels the selection instead of re-selecting it.
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
        // Tapping the already-selected tile again cancels the selection instead of re-selecting it.
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
            _uiState.value = _uiState.value.copy(
                matchingGame = game.copy(matchedIds = matched, selectedLeftId = null, selectedRightId = null),
            )
            if (matched.size == game.pairs.size) {
                viewModelScope.launch {
                    prefs.clearLastSession()
                    _uiState.value = _uiState.value.copy(sessionDone = true, matchingGame = null)
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(
                matchingGame = game.copy(selectedLeftId = null, selectedRightId = null, wrongFlashLeftId = left, wrongFlashRightId = right),
            )
        }
    }

    /** Called after any wrong/typo answer: tracks it for the end-of-session review, and forces
     * an immediate re-queue once the same word has been wrong twice, so it keeps coming back
     * until answered correctly instead of only relying on its originally scheduled repeats. */
    private fun registerMistake(wordId: Long) {
        everWrongIds.add(wordId)
        val count = (wrongCounts[wordId] ?: 0) + 1
        wrongCounts[wordId] = count
        if (count >= 2) {
            queue.add(wordId)
        }
    }

    private fun registerSuccess(wordId: Long) {
        // A correct answer clears the "must retry immediately" pressure, though it
        // stays in everWrongIds so it's still covered by the end-of-session review.
        wrongCounts[wordId] = 0
    }

    fun submitChoice(chosenText: String) {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch {
            val correctRaw = if (prompt.askTermFirst) prompt.word.translation else prompt.word.term
            val wasCorrect = chosenText == com.lexumi.app.domain.usecase.TranslationParser.displayPrimary(correctRaw)
            submitAnswer.submitChoice(prompt.word, wasCorrect)
            if (wasCorrect) { soundFeedbackPlayer.playCorrect(); registerSuccess(prompt.word.id) }
            else { soundFeedbackPlayer.playWrong(); registerMistake(prompt.word.id) }
            _uiState.value = _uiState.value.copy(
                feedback = if (wasCorrect) WordFeedback.Correct else WordFeedback.Wrong(correctRaw.trim()),
                completedCount = _uiState.value.completedCount + 1,
            )
        }
    }

    fun submitTyped(userInput: String) {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch {
            val expected = if (prompt.askTermFirst) prompt.word.translation else prompt.word.term
            val (_, check) = submitAnswer.submitTypedAnswer(prompt.word, userInput, expected)
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

    /** "Вже знаю" — marks the word mastered right away and skips it, removing any other queued repeats of it too. */
    fun markCurrentAsKnown() {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch {
            val updated = prompt.word.copy(score = SCORE_TO_MASTER, level = 1)
            wordRepository.updateWord(updated)
            queue.removeAll { it == prompt.word.id }
            everWrongIds.remove(prompt.word.id)
            advance()
        }
    }

    fun addCurrentToReview() {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch {
            val updated = submitAnswer.toggleReviewList(prompt.word, true)
            // Keep the in-memory prompt in sync, or the next answer submit
            // would overwrite this flag back to false using a stale copy.
            if (_uiState.value.prompt?.word?.id == updated.id) {
                _uiState.value = _uiState.value.copy(prompt = _uiState.value.prompt!!.copy(word = updated))
            }
        }
    }

    /** Saves edits (text and/or picture) to the word currently on screen without losing its learning progress. */
    fun editCurrentWord(term: String, translation: String, imagePath: String?, ruleId: Long?) {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch {
            when (val result = editWord(prompt.word, term, translation, imagePath, ruleId)) {
                is AddResult.Success -> {
                    val updated = prompt.word.copy(term = term.trim(), translation = translation.trim(), imagePath = imagePath, ruleId = ruleId)
                    _uiState.value = _uiState.value.copy(prompt = prompt.copy(word = updated), editError = null)
                }
                AddResult.AlreadyExists -> _uiState.value = _uiState.value.copy(editError = "Таке слово вже є в цій темі")
                AddResult.Blank -> _uiState.value = _uiState.value.copy(editError = "Заповніть слово і переклад")
            }
        }
    }

    /** Deletes the word currently on screen (and any other queued repeats of it) and moves on. */
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

    fun next() {
        viewModelScope.launch { advance() }
    }
}
