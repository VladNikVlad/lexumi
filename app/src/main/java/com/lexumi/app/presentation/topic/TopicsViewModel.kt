package com.lexumi.app.presentation.topic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.model.Topic
import com.lexumi.app.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TopicsViewModel @Inject constructor(
    topicRepository: TopicRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val sectionId: Long = checkNotNull(savedStateHandle["sectionId"])

    val topics: StateFlow<List<Topic>> = topicRepository.observeTopics(sectionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
