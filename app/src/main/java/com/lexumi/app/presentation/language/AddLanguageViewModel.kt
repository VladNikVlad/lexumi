package com.lexumi.app.presentation.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.data.datastore.UserPreferences
import com.lexumi.app.domain.usecase.AddLanguageUseCase
import com.lexumi.app.domain.usecase.AddResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddLanguageViewModel @Inject constructor(
    private val addLanguage: AddLanguageUseCase,
    private val prefs: UserPreferences,
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _createdLanguageId = MutableStateFlow<Long?>(null)
    val createdLanguageId: StateFlow<Long?> = _createdLanguageId

    fun submit(name: String) {
        viewModelScope.launch {
            val profileId = prefs.currentProfileId.first() ?: return@launch
            when (val result = addLanguage(profileId, name)) {
                is AddResult.Success -> {
                    prefs.setSelectedLanguage(result.id)
                    _createdLanguageId.value = result.id
                }
                AddResult.AlreadyExists -> _error.value = "Така мова вже існує"
                AddResult.Blank -> _error.value = "Введіть назву мови"
            }
        }
    }

    fun clearError() { _error.value = null }
}
