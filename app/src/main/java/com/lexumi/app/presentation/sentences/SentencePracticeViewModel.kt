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
import com.lexumi.app.domain.usecase.SentenceChecker
import com.lexumi.app.util.SoundFeedbackPlayer
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
    val result: SentenceChecker.Result? = null,
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
    private val soundFeedbackPlayer: SoundFeedbackPlayer,
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
        _uiState.value = _uiState.value.copy(current = queue.removeAt(0), result = null)
    }

    fun submit(answer: String) {
        val sentence = _uiState.value.current ?: return
        // Try every valid translation, keep whichever one is the closest match.
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
        }

        _uiState.value = _uiState.value.copy(result = best, completed = _uiState.value.completed + 1)
    }

    fun speak(text: String) { ttsManager.speak(text, voiceName) }

    fun next() = advance()
}
