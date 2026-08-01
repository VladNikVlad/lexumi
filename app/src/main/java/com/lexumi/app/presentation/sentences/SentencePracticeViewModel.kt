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

data class SentencePracticeUiState(
    val loading: Boolean = true,
    val current: Sentence? = null,
    val result: SentenceChecker.Result? = null,
    val completed: Int = 0,
    val total: Int = 0,
    val done: Boolean = false,
    val editError: String? = null,
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

    init {
        viewModelScope.launch {
            val all = sentenceRepository.observeSentences(topicId).first()
            queue = all.filter { !it.known }.shuffled().toMutableList()
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
            _uiState.value = _uiState.value.copy(done = true, current = null)
            return
        }
        _uiState.value = _uiState.value.copy(current = queue.removeAt(0), result = null)
    }

    fun submit(answer: String) {
        val sentence = _uiState.value.current ?: return
        val best = sentence.translations
            .map { SentenceChecker.check(answer, it) }
            .minByOrNull {
                when (it.check) {
                    is AnswerCheck.Correct -> 0
                    is AnswerCheck.OneLetterTypo -> 1
                    is AnswerCheck.Wrong -> 2
                }
            }
            ?: SentenceChecker.check(answer, sentence.translations.firstOrNull().orEmpty())

        if (best.check is AnswerCheck.Wrong) soundFeedbackPlayer.playWrong() else soundFeedbackPlayer.playCorrect()

        viewModelScope.launch {
            val wasFullyCorrect = best.check is AnswerCheck.Correct
            val newStreak = if (wasFullyCorrect) sentence.currentStatsStreak + 1 else 0
            val updated = sentence.copy(
                timesSeen = sentence.timesSeen + 1,
                totalCorrect = sentence.totalCorrect + if (wasFullyCorrect) 1 else 0,
                currentStatsStreak = newStreak,
                bestStreak = maxOf(sentence.bestStreak, newStreak),
            )
            sentenceRepository.updateStats(updated)
            if (_uiState.value.current?.id == updated.id) {
                _uiState.value = _uiState.value.copy(current = updated)
            }
        }

        _uiState.value = _uiState.value.copy(result = best, completed = _uiState.value.completed + 1)
    }

    /** "Вже знаю" — marks the sentence known and excludes it from future practice sessions. */
    fun markCurrentAsKnown() {
        val sentence = _uiState.value.current ?: return
        viewModelScope.launch {
            sentenceRepository.updateStats(sentence.copy(known = true))
            advance()
        }
    }

    /** Saves edits to the sentence currently on screen without losing its practice stats. */
    fun editCurrentSentence(text: String, translations: List<String>, ruleIds: List<Long>) {
        val sentence = _uiState.value.current ?: return
        viewModelScope.launch {
            when (editSentence(sentence, text, translations, ruleIds)) {
                is AddResult.Success -> {
                    val updated = sentence.copy(name = text.trim(), text = text.trim(), translations = translations.filter { it.isNotBlank() }, ruleIds = ruleIds)
                    _uiState.value = _uiState.value.copy(current = updated, editError = null)
                }
                AddResult.AlreadyExists -> _uiState.value = _uiState.value.copy(editError = "Таке речення вже є в цій темі")
                AddResult.Blank -> _uiState.value = _uiState.value.copy(editError = "Заповніть речення і хоча б один переклад")
            }
        }
    }

    /** Deletes the sentence currently on screen and moves on to the next one. */
    fun deleteCurrentSentence() {
        val sentence = _uiState.value.current ?: return
        viewModelScope.launch {
            deleteSentence(sentence)
            advance()
        }
    }

    fun clearEditError() { _uiState.value = _uiState.value.copy(editError = null) }

    fun speak(text: String) { ttsManager.speak(text, voiceName) }

    fun next() = advance()
}
