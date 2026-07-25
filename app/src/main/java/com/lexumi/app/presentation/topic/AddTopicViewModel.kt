package com.lexumi.app.presentation.topic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.usecase.AddResult
import com.lexumi.app.domain.usecase.AddTopicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddTopicViewModel @Inject constructor(
    private val addTopic: AddTopicUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val sectionId: Long = checkNotNull(savedStateHandle["sectionId"])

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _createdId = MutableStateFlow<Long?>(null)
    val createdId: StateFlow<Long?> = _createdId

    fun submit(name: String) {
        viewModelScope.launch {
            when (val result = addTopic(sectionId, name)) {
                is AddResult.Success -> _createdId.value = result.id
                AddResult.AlreadyExists -> _error.value = "Така тема в цьому розділі вже існує"
                AddResult.Blank -> _error.value = "Введіть назву теми"
            }
        }
    }

    fun clearError() { _error.value = null }
}
