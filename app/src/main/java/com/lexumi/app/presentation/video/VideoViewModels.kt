package com.lexumi.app.presentation.video

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.model.Rule
import com.lexumi.app.domain.model.TestQuestion
import com.lexumi.app.domain.model.VideoContent
import com.lexumi.app.domain.repository.RuleRepository
import com.lexumi.app.domain.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoListViewModel @Inject constructor(
    videoRepository: VideoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val topicId: Long = checkNotNull(savedStateHandle["topicId"])
    val videos: StateFlow<List<VideoContent>> = videoRepository.observeVideos(topicId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

data class VideoPlayerUiState(
    val video: VideoContent? = null,
    val questions: List<TestQuestion> = emptyList(),
    val rules: List<Rule> = emptyList(),
    val showTranslation: Boolean = false,
)

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val ruleRepository: RuleRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val videoId: Long = checkNotNull(savedStateHandle["videoId"])

    private val _uiState = MutableStateFlow(VideoPlayerUiState())
    val uiState: StateFlow<VideoPlayerUiState> = _uiState

    init {
        viewModelScope.launch {
            val video = videoRepository.getVideo(videoId)
            val questions = videoRepository.getQuestions(videoId)
            val rules = video?.let { ruleRepository.getRulesByIds(it.ruleIds) } ?: emptyList()
            _uiState.value = VideoPlayerUiState(video, questions, rules)
        }
    }

    fun toggleTranslation() {
        _uiState.value = _uiState.value.copy(showTranslation = !_uiState.value.showTranslation)
    }
}
