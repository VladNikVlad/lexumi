package com.lexumi.app.presentation.learnwords

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.data.datastore.UserPreferences
import com.lexumi.app.domain.model.AnswerCheck
import com.lexumi.app.domain.model.Word
import com.lexumi.app.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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

data class LearnWordsUiState(
    val loading: Boolean = true,
    val prompt: WordPrompt? = null,
    val feedback: WordFeedback = WordFeedback.None,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val sessionDone: Boolean = false,
)

@HiltViewModel
class LearnWordsViewModel @Inject constructor(
    private val getSessionWords: GetSessionWordsUseCase,
    private val buildMultipleChoice: BuildMultipleChoiceUseCase,
    private val submitAnswer: SubmitWordAnswerUseCase,
    private val prefs: UserPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val topicId: Long = checkNotNull(savedStateHandle["topicId"])

    private val _uiState = MutableStateFlow(LearnWordsUiState())
    val uiState: StateFlow<LearnWordsUiState> = _uiState

    private var queue: MutableList<Word> = mutableListOf()

    init {
        viewModelScope.launch {
            prefs.setLastSession(topicId, "learn_words")
            val wordsPerSession = prefs.wordsPerSession.first()
            queue = getSessionWords(topicId, wordsPerSession).toMutableList()
            _uiState.value = _uiState.value.copy(totalCount = queue.size, loading = false)
            advance()
        }
    }

    private suspend fun advance() {
        if (queue.isEmpty()) {
            prefs.clearLastSession()
            _uiState.value = _uiState.value.copy(sessionDone = true, prompt = null)
            return
        }
        val word = queue.removeAt(0)
        val askTermFirst = word.askTermFirst()
        val choices = if (word.level == 0) buildMultipleChoice(word, askTermFirst) else null
        _uiState.value = _uiState.value.copy(
            prompt = WordPrompt(word, askTermFirst, choices),
            feedback = WordFeedback.None,
        )
    }

    fun submitChoice(chosenText: String) {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch {
            val correctText = if (prompt.askTermFirst) prompt.word.translation else prompt.word.term
            val wasCorrect = chosenText == correctText
            submitAnswer.submitChoice(prompt.word, wasCorrect)
            _uiState.value = _uiState.value.copy(
                feedback = if (wasCorrect) WordFeedback.Correct else WordFeedback.Wrong(correctText),
                completedCount = _uiState.value.completedCount + 1,
            )
        }
    }

    fun submitTyped(userInput: String) {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch {
            val expected = if (prompt.askTermFirst) prompt.word.translation else prompt.word.term
            val (_, check) = submitAnswer.submitTypedAnswer(prompt.word, userInput, expected)
            val feedback = when (check) {
                is AnswerCheck.Correct -> WordFeedback.Correct
                is AnswerCheck.OneLetterTypo -> WordFeedback.OneLetterTypo(check.correctSpelling)
                is AnswerCheck.Wrong -> WordFeedback.Wrong(check.correctSpelling)
            }
            _uiState.value = _uiState.value.copy(feedback = feedback, completedCount = _uiState.value.completedCount + 1)
        }
    }

    fun addCurrentToReview() {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch { submitAnswer.toggleReviewList(prompt.word, true) }
    }

    fun next() {
        viewModelScope.launch { advance() }
    }
}
