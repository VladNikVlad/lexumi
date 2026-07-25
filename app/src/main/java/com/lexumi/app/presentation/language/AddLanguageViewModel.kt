package com.lexumi.app.presentation.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.data.datastore.UserPreferences
import com.lexumi.app.domain.repository.LanguageRepository
import com.lexumi.app.domain.usecase.AddLanguageUseCase
import com.lexumi.app.domain.usecase.AddResult
import com.lexumi.app.util.LanguageNameResolver
import com.lexumi.app.util.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class AddLanguageViewModel @Inject constructor(
    private val addLanguage: AddLanguageUseCase,
    private val prefs: UserPreferences,
    private val languageRepository: LanguageRepository,
    private val ttsManager: TtsManager,
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
                    autoAssignVoice(result.id, name)
                }
                AddResult.AlreadyExists -> _error.value = "Така мова вже існує"
                AddResult.Blank -> _error.value = "Введіть назву мови"
            }
        }
    }

    /**
     * Finds a free, offline voice already installed on the device that
     * matches the typed language name, so pronunciation sounds natural
     * for each language without the user having to configure anything.
     */
    private suspend fun autoAssignVoice(languageId: Long, name: String) {
        val locale = LanguageNameResolver.resolveLocale(name) ?: return
        val isReady = withTimeoutOrNull(3000) { ttsManager.ready.first { it } } != null
        if (!isReady) return
        val voice = ttsManager.bestVoiceFor(locale) ?: return
        languageRepository.setVoice(languageId, voice.name)
    }

    fun clearError() { _error.value = null }
}
