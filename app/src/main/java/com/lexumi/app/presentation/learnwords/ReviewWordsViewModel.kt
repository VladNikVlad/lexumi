package com.lexumi.app.presentation.learnwords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.model.AnswerCheck
import com.lexumi.app.domain.model.Word
import com.lexumi.app.domain.repository.LanguageRepository
import com.lexumi.app.domain.repository.SectionRepository
import com.lexumi.app.domain.repository.TopicRepository
import com.lexumi.app.domain.repository.WordRepository
import com.lexumi.app.domain.usecase.BuildMultipleChoiceUseCase
import com.lexumi.app.domain.usecase.SubmitWordAnswerUseCase
import com.lexumi.app.domain.usecase.askTermFirst
import com.lexumi.app.util.TtsManager
import com.lexumi.app.util.SoundFeedbackPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewWordsViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val buildMultipleChoice: BuildMultipleChoiceUseCase,
    private val submitAnswer: SubmitWordAnswerUseCase,
    private val topicRepository: TopicRepository,
    private val sectionRepository: SectionRepository,
    private val languageRepository: LanguageRepository,
    private val ttsManager: TtsManager,
    private val soundFeedbackPlayer: SoundFeedbackPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearnWordsUiState())
    val uiState: StateFlow<LearnWordsUiState> = _uiState

    // Review words can come from different topics/languages, so the voice is
    // resolved per-word rather than once for the whole session.
    private val voiceCache = mutableMapOf<Long, String?>()

    private var queue: MutableList<Word> = mutableListOf()

    init {
        viewModelScope.launch {
            val reviewWords = wordRepository.observeReviewList().first()
            queue = reviewWords.toMutableList()
            _uiState.value = _uiState.value.copy(totalCount = queue.size, loading = false)
            advance()
        }
    }

    private suspend fun advance() {
        if (queue.isEmpty()) {
            _uiState.value = _uiState.value.copy(sessionDone = true, prompt = null)
            return
        }
        val word = queue.removeAt(0)
        val askTermFirst = word.askTermFirst()
        val choices = if (word.level == 0) buildMultipleChoice(word, askTermFirst) else null
        _uiState.value = _uiState.value.copy(prompt = WordPrompt(word, askTermFirst, choices), feedback = WordFeedback.None)
    }

    fun submitChoice(chosenText: String) {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch {
            val correctRaw = if (prompt.askTermFirst) prompt.word.translation else prompt.word.term
            val wasCorrect = chosenText == com.lexumi.app.domain.usecase.TranslationParser.displayPrimary(correctRaw)
            submitAnswer.submitChoice(prompt.word, wasCorrect)
            if (wasCorrect) soundFeedbackPlayer.playCorrect() else soundFeedbackPlayer.playWrong()
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
            if (check is AnswerCheck.Wrong) soundFeedbackPlayer.playWrong() else soundFeedbackPlayer.playCorrect()
            val feedback = when (check) {
                is AnswerCheck.Correct -> WordFeedback.Correct
                is AnswerCheck.OneLetterTypo -> WordFeedback.OneLetterTypo(check.correctSpelling)
                is AnswerCheck.Wrong -> WordFeedback.Wrong(check.correctSpelling)
            }
            _uiState.value = _uiState.value.copy(feedback = feedback, completedCount = _uiState.value.completedCount + 1)
        }
    }

    /** Remove from the review list once practiced here, as it already served its purpose. */
    fun removeCurrentFromReview() {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch { submitAnswer.toggleReviewList(prompt.word, false) }
    }

    fun speak(text: String) {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch {
            val voiceName = voiceCache.getOrPut(prompt.word.topicId) {
                val topic = topicRepository.getTopic(prompt.word.topicId)
                val languageId = topic?.let { sectionRepository.getSection(it.sectionId)?.languageId }
                languageId?.let { languageRepository.getLanguage(it)?.voiceName }
            }
            ttsManager.speak(text, voiceName)
        }
    }

    fun next() {
        viewModelScope.launch {
            removeCurrentFromReview()
            advance()
        }
    }
}
