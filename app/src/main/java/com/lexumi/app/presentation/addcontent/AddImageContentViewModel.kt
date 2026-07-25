package com.lexumi.app.presentation.addcontent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.usecase.AddImageContentUseCase
import com.lexumi.app.domain.usecase.AddResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddImageContentViewModel @Inject constructor(
    private val addImage: AddImageContentUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val topicId: Long = checkNotNull(savedStateHandle["topicId"])

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _created = MutableStateFlow(false)
    val created: StateFlow<Boolean> = _created

    fun submit(name: String, imagePath: String?, translation: String) {
        viewModelScope.launch {
            if (imagePath == null) { _error.value = "Виберіть картинку"; return@launch }
            when (addImage(topicId, name, imagePath, translation)) {
                is AddResult.Success -> _created.value = true
                AddResult.AlreadyExists -> _error.value = "Така картинка вже є в цій темі"
                AddResult.Blank -> _error.value = "Введіть назву"
            }
        }
    }

    fun clearError() { _error.value = null }
}
