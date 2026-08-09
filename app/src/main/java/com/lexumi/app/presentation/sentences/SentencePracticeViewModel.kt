package com.lexumi.app.presentation.sentences

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.data.datastore.UserPreferences
import com.lexumi.app.domain.model.AnswerCheck
import com.lexumi.app.domain.model.Rule
import com.lexumi.app.domain.model.Sentence
import com.lexumi.app.domain.repository.LanguageRepository
import com.lexumi.app.domain.repository.RuleRepository
import com.lexumi.app.domain.repository.SectionRepository
import com.lexumi.app.domain.repository.SentenceRepository
import com.lexumi.app.domain.repository.TopicRepository
import com.lexumi.app.domain.usecase.AddResult
import com.lexumi.app.domain.usecase.DeleteSentenceUseCase
import com.lexumi.app.domain.usecase.EditSentenceUseCase
import com.lexumi.app.domain.usecase.GetSessionSentencesUseCase
import com.lexumi.app.domain.usecase.SentenceChecker
import com.lexumi.app.domain.usecase.SubmitSentenceAnswerUseCase
import com.lexumi.app.domain.usecase.askOriginalFirst
import com.lexumi.app.domain.usecase.isAudioOnly
import com.lexumi.app.domain.usecase.isVoiceOnly
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

data class SentencePrompt(
    val sentence: Sentence,
    /** true: target text/audio shown, native answer expected (ratings 0 & 2). false: native text
     * shown, target-language answer expected (ratings 1 & 3). */
    val askOriginalFirst: Boolean,
    /** What's shown on screen — null for rating 2 (audio-only, nothing shown, only heard). */
    val displayText: String?,
    /** What the auto-read-aloud / replay button says. */
    val speakText: String,
    /** Rating 2: no text at all, only TTS. */
    val audioOnly: Boolean = false,
    /** Rating 3: mastered — no more typing, only speaking the answer out loud counts. */
    val voiceOnly: Boolean = false,
)

data class HintState(
    val correctWords: List<String>,
    val blankIndices: Set<Int>,
    val inputs: Map<Int, String> = emptyMap(),
    val wrongFlash: Set<Int> = emptySet(),
    /** Blanks the user tapped to reveal — shows the correct word above that one field. */
    val revealed: Set<Int> = emptySet(),
    /** Failed "Перевірити" taps so far — after 2 we stop asking and just move on. */
    val attempts: Int = 0,
)

data class SentencePracticeUiState(
    val loading: Boolean = true,
    val prompt: SentencePrompt? = null,
    val result: SentenceChecker.Result? = null,
    val hint: HintState? = null,
    val completed: Int = 0,
    val total: Int = 0,
    val done: Boolean = false,
    val editError: String? = null,
    val inMistakeReview: Boolean = false,
    /** Voice-only (rating 3) prompt: whether we're actively listening, and what was last heard. */
    val listening: Boolean = false,
    val heard: String? = null,
    /** True once "Я зараз не можу говорити" was pressed — no more rating-3 sentences this session. */
    val voiceDisabled: Boolean = false,
    /** Live status/partial/error text from the recognizer — for debugging why recognition isn't working. */
    val voiceDebug: String? = null,
)

@HiltViewModel
class SentencePracticeViewModel @Inject constructor(
    private val sentenceRepository: SentenceRepository,
    private val topicRepository: TopicRepository,
    private val sectionRepository: SectionRepository,
    private val languageRepository: LanguageRepository,
    private val editSentence: EditSentenceUseCase,
    private val deleteSentence: DeleteSentenceUseCase,
    private val getSessionSentences: GetSessionSentencesUseCase,
    private val submitAnswer: SubmitSentenceAnswerUseCase,
    ruleRepository: RuleRepository,
    private val prefs: UserPreferences,
    private val ttsManager: TtsManager,
    private val voiceRecognizer: VoiceRecognizerManager,
    private val soundFeedbackPlayer: SoundFeedbackPlayer,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val topicId: Long = checkNotNull(savedStateHandle["topicId"])

    private val _uiState = MutableStateFlow(SentencePracticeUiState())
    val uiState: StateFlow<SentencePracticeUiState> = _uiState

    private val _languageId = MutableStateFlow<Long?>(null)
    val rules: StateFlow<List<Rule>> = _languageId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else ruleRepository.observeRules(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var queue: MutableList<Long> = mutableListOf()
    private var voiceName: String? = null

    private val everWrongIds = mutableSetOf<Long>()
    private var mistakeReviewStarted = false
    private var allSentencesById: Map<Long, Sentence> = emptyMap()

    // "Я зараз не можу говорити" — once pressed, rating-3 sentences are skipped for the rest
    // of this session (they come back untouched next session).
    private var voiceDisabledThisSession = false

    companion object {
        /** After this many failed "Перевірити" taps on the fill-in-the-blanks retry, stop and move on. */
        private const val MAX_HINT_ATTEMPTS = 2
    }

    init {
        viewModelScope.launch {
            prefs.setLastSession(topicId, "sentence_practice")
            // Same "words/sentences per session" and "repetitions" settings as word learning.
            val perSession = prefs.wordsPerSession.first()
            val repetitions = prefs.repetitions.first()
            val all = sentenceRepository.getSentences(topicId).filter { !it.known }
            allSentencesById = all.associateBy { it.id }
            queue = getSessionSentences(topicId, perSession, repetitions).toMutableList()
            _uiState.value = _uiState.value.copy(total = queue.size, loading = false)
            advance()

            val topic = topicRepository.getTopic(topicId)
            val languageId = topic?.let { sectionRepository.getSection(it.sectionId)?.languageId }
            _languageId.value = languageId
            voiceName = languageId?.let { languageRepository.getLanguage(it)?.voiceName }
        }
    }

    private fun advance() {
        if (queue.isEmpty()) {
            if (!mistakeReviewStarted && everWrongIds.isNotEmpty()) {
                mistakeReviewStarted = true
                queue = everWrongIds.toMutableList().apply { shuffle() }
                _uiState.value = _uiState.value.copy(inMistakeReview = true)
            } else {
                viewModelScope.launch { prefs.clearLastSession() }
                _uiState.value = _uiState.value.copy(done = true, prompt = null)
                return
            }
        }
        // Re-fetch fresh each time — an earlier repeat this session may have advanced the rating.
        val sentenceId = queue.removeAt(0)
        val sentence = allSentencesById[sentenceId] ?: run { advance(); return }
        if (sentence.rating == 3 && voiceDisabledThisSession) { advance(); return } // skip silently, comes back next session
        _uiState.value = _uiState.value.copy(
            prompt = buildPrompt(sentence),
            result = null,
            hint = null,
            listening = false,
            heard = null,
        )
    }

    private fun buildPrompt(sentence: Sentence): SentencePrompt {
        val askOriginal = sentence.askOriginalFirst()
        val audioOnly = sentence.isAudioOnly()
        val text = if (askOriginal) sentence.text else (sentence.translations.firstOrNull() ?: sentence.text)
        return SentencePrompt(
            sentence = sentence,
            askOriginalFirst = askOriginal,
            displayText = if (audioOnly) null else text,
            speakText = text,
            audioOnly = audioOnly,
            voiceOnly = sentence.isVoiceOnly(),
        )
    }

    private fun registerMistake(sentenceId: Long) { everWrongIds.add(sentenceId) }
    private fun registerSuccess(sentenceId: Long) { /* still covered by the single end-of-session review */ }

    /** The set of acceptable target strings for the current prompt's direction. */
    private fun targetOptions(prompt: SentencePrompt): List<String> =
        if (prompt.askOriginalFirst) prompt.sentence.translations else listOf(prompt.sentence.text)

    private fun bestCheck(prompt: SentencePrompt, answer: String): SentenceChecker.Result =
        targetOptions(prompt)
            .map { SentenceChecker.check(answer, it) }
            .minByOrNull {
                when (it.category) {
                    SentenceChecker.Category.CORRECT -> 0
                    SentenceChecker.Category.PARTIAL -> 1
                    SentenceChecker.Category.WRONG -> 2
                }
            }
            ?: SentenceChecker.check(answer, targetOptions(prompt).firstOrNull().orEmpty())

    fun submit(answer: String) {
        val prompt = _uiState.value.prompt ?: return
        if (prompt.voiceOnly) return // typing is disabled once mastered — use startListeningForSentence() instead
        val sentence = prompt.sentence
        val best = bestCheck(prompt, answer)

        when (best.category) {
            SentenceChecker.Category.CORRECT -> {
                soundFeedbackPlayer.playCorrect()
                registerSuccess(sentence.id)
                finalizeAttempt(sentence, wasCorrect = true)
                _uiState.value = _uiState.value.copy(result = best, completed = _uiState.value.completed + 1)
            }
            SentenceChecker.Category.PARTIAL -> {
                // One or two words off — offer a fill-in-the-blanks hint for just those words,
                // instead of failing the whole sentence outright.
                soundFeedbackPlayer.playWrong()
                registerMistake(sentence.id)
                _uiState.value = _uiState.value.copy(
                    hint = HintState(correctWords = best.correctWords, blankIndices = best.mismatchedIndices.toSet()),
                    completed = _uiState.value.completed + 1,
                )
            }
            SentenceChecker.Category.WRONG -> {
                soundFeedbackPlayer.playWrong()
                registerMistake(sentence.id)
                finalizeAttempt(sentence, wasCorrect = false)
                _uiState.value = _uiState.value.copy(result = best, completed = _uiState.value.completed + 1)
            }
        }
    }

    /** Rating-3 flow: listens once, stops itself after ~2s of silence, and checks whatever was
     * heard — no typing, no hint retry, straight to the result. */
    fun startListeningForSentence() {
        val prompt = _uiState.value.prompt ?: return
        if (!prompt.voiceOnly || voiceDisabledThisSession) return
        _uiState.value = _uiState.value.copy(listening = true, heard = null, voiceDebug = null)
        val locale = ttsManager.localeFor(voiceName)
        voiceRecognizer.listenOnce(
            locale = locale,
            onPartial = { partial -> _uiState.value = _uiState.value.copy(voiceDebug = "Чую: «$partial»") },
            onDebug = { line -> _uiState.value = _uiState.value.copy(voiceDebug = line) },
            onResult = { heardRaw ->
                _uiState.value = _uiState.value.copy(listening = false, heard = heardRaw)
                submitVoiceAnswer(heardRaw)
            },
        )
    }

    private fun submitVoiceAnswer(heardRaw: String) {
        val prompt = _uiState.value.prompt ?: return
        val sentence = prompt.sentence
        val best = bestCheck(prompt, heardRaw)
        val wasCorrect = best.category == SentenceChecker.Category.CORRECT
        if (wasCorrect) { soundFeedbackPlayer.playCorrect(); registerSuccess(sentence.id) }
        else { soundFeedbackPlayer.playWrong(); registerMistake(sentence.id) }
        finalizeAttempt(sentence, wasCorrect)
        _uiState.value = _uiState.value.copy(result = best, completed = _uiState.value.completed + 1)
    }

    /** "Я зараз не можу говорити" — this and every other rating-3 sentence disappears until next session. */
    fun disableVoiceForSession() {
        voiceDisabledThisSession = true
        voiceRecognizer.stop()
        _uiState.value = _uiState.value.copy(voiceDisabled = true)
        viewModelScope.launch { advance() } // the current (voice-only) prompt is skipped too
    }

    fun updateHintInput(index: Int, value: String) {
        val hint = _uiState.value.hint ?: return
        _uiState.value = _uiState.value.copy(hint = hint.copy(inputs = hint.inputs + (index to value), wrongFlash = hint.wrongFlash - index))
    }

    /** Tapping a still-blank word reveals the correct word above it, same idea as the word-learning hint. */
    fun toggleHintReveal(index: Int) {
        val hint = _uiState.value.hint ?: return
        _uiState.value = _uiState.value.copy(
            hint = hint.copy(revealed = if (index in hint.revealed) hint.revealed - index else hint.revealed + index),
        )
    }

    /** Checks the currently filled-in blanks; correct ones lock in, wrong ones flash red and stay open.
     * After [MAX_HINT_ATTEMPTS] failed tries the sentence is given up on automatically — the correct
     * answer is shown and the user moves on, instead of being stuck retyping the same word forever. */
    fun submitHints() {
        val hint = _uiState.value.hint ?: return
        val prompt = _uiState.value.prompt ?: return
        val stillWrong = mutableSetOf<Int>()
        val nowCorrect = mutableSetOf<Int>()
        for (index in hint.blankIndices) {
            val typed = hint.inputs[index].orEmpty()
            val check = SentenceChecker.checkSingleWord(typed, hint.correctWords[index])
            if (check is AnswerCheck.Wrong) stillWrong.add(index) else nowCorrect.add(index)
        }
        val remainingBlanks = hint.blankIndices - nowCorrect
        val attempts = hint.attempts + 1
        when {
            remainingBlanks.isEmpty() -> {
                // All blanks filled correctly — it still needed help, so the streak resets
                // (matches "any miss resets the streak"), but the sentence isn't outright wrong either.
                finalizeAttempt(prompt.sentence, wasCorrect = false)
                _uiState.value = _uiState.value.copy(
                    hint = null,
                    result = SentenceChecker.Result(SentenceChecker.Category.CORRECT, AnswerCheck.Correct, hint.correctWords, emptyList(), emptyList()),
                )
            }
            attempts >= MAX_HINT_ATTEMPTS -> {
                // Tried twice and still wrong — stop asking, reveal the answer, move on.
                finalizeAttempt(prompt.sentence, wasCorrect = false)
                _uiState.value = _uiState.value.copy(
                    hint = null,
                    result = SentenceChecker.Result(
                        category = SentenceChecker.Category.WRONG,
                        check = AnswerCheck.Wrong(hint.correctWords.joinToString(" ")),
                        correctWords = hint.correctWords,
                        mismatchedIndices = remainingBlanks.toList(),
                        userWordsAtMismatches = remainingBlanks.map { hint.inputs[it].orEmpty() },
                    ),
                )
            }
            else -> {
                _uiState.value = _uiState.value.copy(hint = hint.copy(blankIndices = remainingBlanks, wrongFlash = stillWrong, attempts = attempts))
            }
        }
    }

    private fun finalizeAttempt(sentence: Sentence, wasCorrect: Boolean) {
        viewModelScope.launch {
            val updated = submitAnswer.submit(sentence, wasCorrect)
            allSentencesById = allSentencesById + (updated.id to updated)
            if (_uiState.value.prompt?.sentence?.id == updated.id) {
                _uiState.value = _uiState.value.copy(prompt = _uiState.value.prompt!!.copy(sentence = updated))
            }
        }
    }

    /** "Вже знаю": ratings 0-1 jump to the audio round (2); ratings 2-3 jump to mastered (4). */
    fun markCurrentAsKnown() {
        val sentence = _uiState.value.prompt?.sentence ?: return
        viewModelScope.launch {
            submitAnswer.markAsKnown(sentence)
            queue.removeAll { it == sentence.id }
            everWrongIds.remove(sentence.id)
            advance()
        }
    }

    /** Saves edits to the sentence currently on screen without losing its practice stats. */
    fun editCurrentSentence(text: String, translations: List<String>, ruleIds: List<Long>) {
        val sentence = _uiState.value.prompt?.sentence ?: return
        viewModelScope.launch {
            when (editSentence(sentence, text, translations, ruleIds)) {
                is AddResult.Success -> {
                    val updated = sentence.copy(name = text.trim(), text = text.trim(), translations = translations.filter { it.isNotBlank() }, ruleIds = ruleIds)
                    allSentencesById = allSentencesById + (updated.id to updated)
                    _uiState.value = _uiState.value.copy(prompt = buildPrompt(updated), editError = null)
                }
                AddResult.AlreadyExists -> _uiState.value = _uiState.value.copy(editError = "Таке речення вже є в цій темі")
                AddResult.Blank -> _uiState.value = _uiState.value.copy(editError = "Заповніть речення і хоча б один переклад")
            }
        }
    }

    /** Deletes the sentence currently on screen and moves on to the next one. */
    fun deleteCurrentSentence() {
        val sentence = _uiState.value.prompt?.sentence ?: return
        viewModelScope.launch {
            queue.removeAll { it == sentence.id }
            everWrongIds.remove(sentence.id)
            deleteSentence(sentence)
            advance()
        }
    }

    fun clearEditError() { _uiState.value = _uiState.value.copy(editError = null) }

    fun speak(text: String) { ttsManager.speak(text, voiceName) }

    fun next() = advance()

    override fun onCleared() {
        voiceRecognizer.stop()
        super.onCleared()
    }
}
