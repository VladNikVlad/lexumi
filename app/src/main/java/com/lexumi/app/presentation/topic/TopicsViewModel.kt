package com.lexumi.app.presentation.topic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.model.Topic
import com.lexumi.app.domain.repository.SectionRepository
import com.lexumi.app.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopicsViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val sectionRepository: SectionRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val sectionId: Long = checkNotNull(savedStateHandle["sectionId"])

    val topics: StateFlow<List<Topic>> = topicRepository.observeTopics(sectionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Fail-closed: hidden until the section's own remoteId is known — a section synced from the
    // admin (remoteId != null) is always read-only, one the user created themselves is always
    // editable. No admin check anymore: editing admin content is a web-panel-only job now.
    private val _canEdit = MutableStateFlow(false)
    val canEdit: StateFlow<Boolean> = _canEdit

    init {
        viewModelScope.launch {
            _canEdit.value = sectionRepository.getSection(sectionId)?.remoteId == null
        }
    }

    /** Drag-and-drop reorder — [orderedIds] is the full, final top-to-bottom order. */
    fun reorder(orderedIds: List<Long>) {
        viewModelScope.launch { topicRepository.reorderTopics(orderedIds) }
    }
}
