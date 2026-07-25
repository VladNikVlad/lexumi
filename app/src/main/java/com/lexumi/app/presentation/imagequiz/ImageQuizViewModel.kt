package com.lexumi.app.presentation.imagequiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.model.ImageContent
import com.lexumi.app.domain.repository.ImageContentRepository
import com.lexumi.app.util.SoundFeedbackPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImageQuizPrompt(val image: ImageContent, val options: List<String>)

data class ImageQuizUiState(
    val loading: Boolean = true,
    val prompt: ImageQuizPrompt? = null,
    val feedbackCorrect: Boolean? = null,
    val completed: Int = 0,
    val total: Int = 0,
    val done: Boolean = false,
)

@HiltViewModel
class ImageQuizViewModel @Inject constructor(
    private val imageRepository: ImageContentRepository,
    private val soundFeedbackPlayer: SoundFeedbackPlayer,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val topicId: Long = checkNotNull(savedStateHandle["topicId"])

    private val _uiState = MutableStateFlow(ImageQuizUiState())
    val uiState: StateFlow<ImageQuizUiState> = _uiState

    private var all: List<ImageContent> = emptyList()
    private var queue: MutableList<ImageContent> = mutableListOf()

    init {
        viewModelScope.launch {
            all = imageRepository.observeImages(topicId).first()
            queue = all.shuffled().toMutableList()
            _uiState.value = _uiState.value.copy(total = queue.size, loading = false)
            advance()
        }
    }

    private fun advance() {
        if (queue.isEmpty()) {
            _uiState.value = _uiState.value.copy(done = true, prompt = null)
            return
        }
        val image = queue.removeAt(0)
        val distractors = all.filter { it.id != image.id }.map { it.translation }.shuffled().take(3)
        val options = (distractors + image.translation).shuffled()
        _uiState.value = _uiState.value.copy(prompt = ImageQuizPrompt(image, options), feedbackCorrect = null)
    }

    fun submit(answer: String) {
        val prompt = _uiState.value.prompt ?: return
        val correct = answer == prompt.image.translation
        if (correct) soundFeedbackPlayer.playCorrect() else soundFeedbackPlayer.playWrong()
        _uiState.value = _uiState.value.copy(feedbackCorrect = correct, completed = _uiState.value.completed + 1)
    }

    fun next() = advance()
}
