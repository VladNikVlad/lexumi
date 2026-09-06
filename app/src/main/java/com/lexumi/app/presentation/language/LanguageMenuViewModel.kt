package com.lexumi.app.presentation.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.data.auth.AuthRepository
import com.lexumi.app.data.datastore.UserPreferences
import com.lexumi.app.data.sync.ContentSyncRepository
import com.lexumi.app.data.sync.DownloadableLanguage
import com.lexumi.app.domain.model.Language
import com.lexumi.app.domain.repository.LanguageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LanguageMenuUiState(
    val isAdmin: Boolean = false,
    val downloadableLanguages: List<DownloadableLanguage> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class LanguageMenuViewModel @Inject constructor(
    private val languageRepository: LanguageRepository,
    private val prefs: UserPreferences,
    private val authRepository: AuthRepository,
    private val syncRepository: ContentSyncRepository,
) : ViewModel() {

    // Shared across all local profiles on this device (point 3 of the settings rework) —
    // languages aren't filtered by the active profile anymore.
    val languages: StateFlow<List<Language>> = languageRepository.observeLanguages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selected = MutableStateFlow<Long?>(null)
    val selected: StateFlow<Long?> = _selected

    private val _uiState = MutableStateFlow(LanguageMenuUiState())
    val uiState: StateFlow<LanguageMenuUiState> = _uiState

    init {
        viewModelScope.launch {
            val profileResult = runCatching { authRepository.getMyProfile() }
            val isAdmin = profileResult.getOrNull()?.isAdmin == true
            _uiState.value = _uiState.value.copy(
                isAdmin = isAdmin,
                message = profileResult.exceptionOrNull()?.let { "Не вдалося перевірити статус адміна: ${it.message}" },
            )
            if (!isAdmin) refreshDownloadable()
        }
    }

    private suspend fun refreshDownloadable() {
        val downloadable = runCatching { syncRepository.listDownloadableLanguages() }.getOrDefault(emptyList())
        _uiState.value = _uiState.value.copy(downloadableLanguages = downloadable)
    }

    fun selectLanguage(languageId: Long) {
        viewModelScope.launch {
            prefs.setSelectedLanguage(languageId)
            _selected.value = languageId
        }
    }

    /** Admin: pushes this language (and everything under it) to Supabase as global content. */
    fun publish(languageId: Long) {
        if (_uiState.value.busy) return
        _uiState.value = _uiState.value.copy(busy = true, message = null)
        viewModelScope.launch {
            val result = runCatching { syncRepository.publishLanguage(languageId) }
            val skipped = result.getOrNull()?.skippedVideoNames.orEmpty()
            _uiState.value = _uiState.value.copy(
                busy = false,
                message = when {
                    result.isFailure -> "Не вдалося опублікувати: ${result.exceptionOrNull()?.message}"
                    skipped.isNotEmpty() -> "Опубліковано (без відео без YouTube-посилання: ${skipped.joinToString(", ")})"
                    else -> "Опубліковано"
                },
            )
        }
    }

    /** Regular user: downloads an admin-published language into their own local copy, then opens it. */
    fun download(remoteLanguageId: String) {
        if (_uiState.value.busy) return
        _uiState.value = _uiState.value.copy(busy = true, message = null)
        viewModelScope.launch {
            val profileId = prefs.currentProfileId.first()
            if (profileId == null) {
                _uiState.value = _uiState.value.copy(busy = false, message = "Спершу створіть профіль")
                return@launch
            }
            val result = runCatching { syncRepository.downloadLanguage(remoteLanguageId, profileId) }
            _uiState.value = _uiState.value.copy(busy = false, message = if (result.isSuccess) null else "Не вдалося завантажити: ${result.exceptionOrNull()?.message}")
            result.getOrNull()?.let { newLocalId ->
                refreshDownloadable()
                selectLanguage(newLocalId)
            }
        }
    }
}
