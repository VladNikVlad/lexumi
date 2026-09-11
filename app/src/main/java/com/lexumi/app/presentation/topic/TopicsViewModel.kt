package com.lexumi.app.presentation.topic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.model.Topic
import com.lexumi.app.domain.repository.SectionRepository
import com.lexumi.app.domain.repository.TopicRepository
import com.lexumi.app.domain.usecase.IsLanguageEditableUseCase
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
    private val isLanguageEditable: IsLanguageEditableUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val sectionId: Long = checkNotNull(savedStateHandle["sectionId"])

    val topics: StateFlow<List<Topic>> = topicRepository.observeTopics(sectionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Fail-closed: hidden until the async check resolves — see HomeViewModel for why.
    private val _canEdit = MutableStateFlow(false)
    val canEdit: StateFlow<Boolean> = _canEdit

    init {
        viewModelScope.launch {
            val languageId = sectionRepository.getSection(sectionId)?.languageId
            _canEdit.value = languageId == null || isLanguageEditable(languageId)
        }
    }

    /** Drag-and-drop reorder — [orderedIds] is the full, final top-to-bottom order. */
    fun reorder(orderedIds: List<Long>) {
        viewModelScope.launch { topicRepository.reorderTopics(orderedIds) }
    }
}
