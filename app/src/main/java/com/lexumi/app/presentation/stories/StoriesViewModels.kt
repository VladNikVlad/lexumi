package com.lexumi.app.presentation.stories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.model.Rule
import com.lexumi.app.domain.model.Story
import com.lexumi.app.domain.repository.RuleRepository
import com.lexumi.app.domain.repository.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoriesListViewModel @Inject constructor(
    storyRepository: StoryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val topicId: Long = checkNotNull(savedStateHandle["topicId"])
    val stories: StateFlow<List<Story>> = storyRepository.observeStories(topicId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

data class StoryReaderUiState(
    val story: Story? = null,
    val rules: List<Rule> = emptyList(),
    val showTranslation: Boolean = false,
)

@HiltViewModel
class StoryReaderViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val ruleRepository: RuleRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val storyId: Long = checkNotNull(savedStateHandle["storyId"])

    private val _uiState = MutableStateFlow(StoryReaderUiState())
    val uiState: StateFlow<StoryReaderUiState> = _uiState

    init {
        viewModelScope.launch {
            val story = storyRepository.getStory(storyId)
            val rules = story?.let { ruleRepository.getRulesByIds(it.ruleIds) } ?: emptyList()
            _uiState.value = StoryReaderUiState(story, rules)
        }
    }

    fun toggleTranslation() { _uiState.value = _uiState.value.copy(showTranslation = !_uiState.value.showTranslation) }
}
