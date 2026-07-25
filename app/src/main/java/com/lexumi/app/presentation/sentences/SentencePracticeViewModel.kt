package com.lexumi.app.presentation.sentences

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.model.AnswerCheck
import com.lexumi.app.domain.model.Sentence
import com.lexumi.app.domain.repository.LanguageRepository
import com.lexumi.app.domain.repository.SectionRepository
import com.lexumi.app.domain.repository.SentenceRepository
import com.lexumi.app.domain.repository.TopicRepository
import com.lexumi.app.domain.usecase.AnswerChecker
import com.lexumi.app.util.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SentencePracticeUiState(
    val loading: Boolean = true,
    val current: Sentence? = null,
    val feedback: AnswerCheck? = null,
    val completed: Int = 0,
    val total: Int = 0,
    val done: Boolean = false,
)

@HiltViewModel
class SentencePracticeViewModel @Inject constructor(
    private val sentenceRepository: SentenceRepository,
    private val topicRepository: TopicRepository,
    private val sectionRepository: SectionRepository,
    private val languageRepository: LanguageRepository,
    private val ttsManager: TtsManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val topicId: Long = checkNotNull(savedStateHandle["topicId"])

    private val _uiState = MutableStateFlow(SentencePracticeUiState())
    val uiState: StateFlow<SentencePracticeUiState> = _uiState

    private var queue: MutableList<Sentence> = mutableListOf()
    private var voiceName: String? = null

    init {
        viewModelScope.launch {
            val all = sentenceRepository.observeSentences(topicId).first()
            queue = all.shuffled().toMutableList()
            _uiState.value = _uiState.value.copy(total = queue.size, loading = false)
            advance()

            val topic = topicRepository.getTopic(topicId)
            val languageId = topic?.let { sectionRepository.getSection(it.sectionId)?.languageId }
            voiceName = languageId?.let { languageRepository.getLanguage(it)?.voiceName }
        }
    }

    private fun advance() {
        if (queue.isEmpty()) {
            _uiState.value = _uiState.value.copy(done = true, current = null)
            return
        }
        _uiState.value = _uiState.value.copy(current = queue.removeAt(0), feedback = null)
    }

    fun submit(answer: String) {
        val sentence = _uiState.value.current ?: return
        // accept the closest of the valid translations
        val best = sentence.translations.minByOrNull {
            when (val check = AnswerChecker.check(answer, it)) {
                is AnswerCheck.Correct -> 0
                is AnswerCheck.OneLetterTypo -> 1
                is AnswerCheck.Wrong -> 2
            }
        } ?: sentence.translations.firstOrNull() ?: ""
        val check = AnswerChecker.check(answer, best)
        _uiState.value = _uiState.value.copy(feedback = check, completed = _uiState.value.completed + 1)
    }

    fun speak(text: String) { ttsManager.speak(text, voiceName) }

    fun next() = advance()
}
