package com.lexumi.app.presentation.section

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.usecase.AddResult
import com.lexumi.app.domain.usecase.AddSectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddSectionViewModel @Inject constructor(
    private val addSection: AddSectionUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val languageId: Long = checkNotNull(savedStateHandle["languageId"])

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _createdId = MutableStateFlow<Long?>(null)
    val createdId: StateFlow<Long?> = _createdId

    fun submit(name: String) {
        viewModelScope.launch {
            when (val result = addSection(languageId, name)) {
                is AddResult.Success -> _createdId.value = result.id
                AddResult.AlreadyExists -> _error.value = "Такий розділ у цій мові вже існує"
                AddResult.Blank -> _error.value = "Введіть назву розділу"
            }
        }
    }

    fun clearError() { _error.value = null }
}
