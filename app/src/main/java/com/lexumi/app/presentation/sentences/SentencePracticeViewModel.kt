package com.lexumi.app.presentation.sentences

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.lexumi.app.domain.usecase.SentenceChecker
import com.lexumi.app.domain.usecase.askOriginalFirst
import com.lexumi.app.util.SoundFeedbackPlayer
import com.lexumi.app.util.TtsManager
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
    val askOriginalFirst: Boolean,
    val displayText: String,
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
)

@HiltViewModel
class SentencePracticeViewModel @Inject constructor(
    private val sentenceRepository: SentenceRepository,
    private val topicRepository: TopicRepository,
    private val sectionRepository: SectionRepository,
    private val languageRepository: LanguageRepository,
    private val editSentence: EditSentenceUseCase,
    private val deleteSentence: DeleteSentenceUseCase,
    ruleRepository: RuleRepository,
    private val ttsManager: TtsManager,
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

    private var queue: MutableList<Sentence> = mutableListOf()
    private var voiceName: String? = null

    // Session-only mistake tracking, same rules as the word engine: retry a
    // sentence right away once it's been wrong twice, and do a final
    // "work on mistakes" pass over anything that was ever wrong.
    private val wrongCounts = mutableMapOf<Long, Int>()
    private val everWrongIds = mutableSetOf<Long>()
    private var mistakeReviewStarted = false
    private var allSentencesById: Map<Long, Sentence> = emptyMap()

    companion object {
        /** After this many failed "Перевірити" taps on the fill-in-the-blanks retry, stop and move on. */
        private const val MAX_HINT_ATTEMPTS = 2
    }

    init {
        viewModelScope.launch {
            val all = sentenceRepository.observeSentences(topicId).first().filter { !it.known }
            allSentencesById = all.associateBy { it.id }
            queue = all.shuffled().toMutableList()
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
                queue = everWrongIds.mapNotNull { allSentencesById[it] }.toMutableList().apply { shuffle() }
                _uiState.value = _uiState.value.copy(inMistakeReview = true)
            } else {
                _uiState.value = _uiState.value.copy(done = true, prompt = null)
                return
            }
        }
        val sentence = queue.removeAt(0)
        val askOriginal = sentence.askOriginalFirst()
        val displayText = if (askOriginal) sentence.text else (sentence.translations.firstOrNull() ?: sentence.text)
        _uiState.value = _uiState.value.copy(
            prompt = SentencePrompt(sentence, askOriginal, displayText),
            result = null,
            hint = null,
        )
    }

    private fun registerMistake(sentenceId: Long) {
        everWrongIds.add(sentenceId)
        val count = (wrongCounts[sentenceId] ?: 0) + 1
        wrongCounts[sentenceId] = count
        if (count >= 2) {
            allSentencesById[sentenceId]?.let { queue.add(it) }
        }
    }

    private fun registerSuccess(sentenceId: Long) {
        wrongCounts[sentenceId] = 0
    }

    /** The set of acceptable target strings for the current prompt's direction. */
    private fun targetOptions(prompt: SentencePrompt): List<String> =
        if (prompt.askOriginalFirst) prompt.sentence.translations else listOf(prompt.sentence.text)

    fun submit(answer: String) {
        val prompt = _uiState.value.prompt ?: return
        val sentence = prompt.sentence

        val best = targetOptions(prompt)
            .map { SentenceChecker.check(answer, it) }
            .minByOrNull {
                when (it.category) {
                    SentenceChecker.Category.CORRECT -> 0
                    SentenceChecker.Category.PARTIAL -> 1
                    SentenceChecker.Category.WRONG -> 2
                }
            }
            ?: SentenceChecker.check(answer, targetOptions(prompt).firstOrNull().orEmpty())

        when (best.category) {
            SentenceChecker.Category.CORRECT -> {
                soundFeedbackPlayer.playCorrect()
                registerSuccess(sentence.id)
                finalizeAttempt(sentence, wasFullyCorrect = true)
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
                finalizeAttempt(sentence, wasFullyCorrect = false)
                _uiState.value = _uiState.value.copy(result = best, completed = _uiState.value.completed + 1)
            }
        }
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
                // All blanks filled correctly — this attempt still counts as a
                // mistake overall (it needed help), matching the "retry until
                // correct" and end-of-session mistake-review rules.
                finalizeAttempt(prompt.sentence, wasFullyCorrect = false)
                _uiState.value = _uiState.value.copy(
                    hint = null,
                    result = SentenceChecker.Result(SentenceChecker.Category.CORRECT, AnswerCheck.Correct, hint.correctWords, emptyList(), emptyList()),
                )
            }
            attempts >= MAX_HINT_ATTEMPTS -> {
                // Tried twice and still wrong — stop asking, reveal the answer, move on.
                finalizeAttempt(prompt.sentence, wasFullyCorrect = false)
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

    private fun finalizeAttempt(sentence: Sentence, wasFullyCorrect: Boolean) {
        viewModelScope.launch {
            val newStreak = if (wasFullyCorrect) sentence.currentStatsStreak + 1 else 0
            val updated = sentence.copy(
                timesSeen = sentence.timesSeen + 1,
                totalCorrect = sentence.totalCorrect + if (wasFullyCorrect) 1 else 0,
                currentStatsStreak = newStreak,
                bestStreak = maxOf(sentence.bestStreak, newStreak),
            )
            sentenceRepository.updateStats(updated)
            allSentencesById = allSentencesById + (updated.id to updated)
            if (_uiState.value.prompt?.sentence?.id == updated.id) {
                _uiState.value = _uiState.value.copy(prompt = _uiState.value.prompt!!.copy(sentence = updated))
            }
        }
    }

    /** "Вже знаю" — marks the sentence known and excludes it from future practice sessions. */
    fun markCurrentAsKnown() {
        val sentence = _uiState.value.prompt?.sentence ?: return
        viewModelScope.launch {
            sentenceRepository.updateStats(sentence.copy(known = true))
            queue.removeAll { it.id == sentence.id }
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
                    _uiState.value = _uiState.value.copy(
                        prompt = _uiState.value.prompt?.copy(sentence = updated, displayText = if (_uiState.value.prompt?.askOriginalFirst == true) updated.text else (updated.translations.firstOrNull() ?: updated.text)),
                        editError = null,
                    )
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
            queue.removeAll { it.id == sentence.id }
            everWrongIds.remove(sentence.id)
            deleteSentence(sentence)
            advance()
        }
    }

    fun clearEditError() { _uiState.value = _uiState.value.copy(editError = null) }

    fun speak(text: String) { ttsManager.speak(text, voiceName) }

    fun next() = advance()
}
