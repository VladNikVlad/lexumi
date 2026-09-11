package com.lexumi.app.presentation.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

/** Postgrest/ktor exception messages can include the full failed HTTP request for debugging —
 * headers and all, which means a live `Authorization: Bearer <token>` ends up in this string.
 * Never show that in a user-facing error message; cut it off at the first header dump. */
private fun sanitizeSyncError(message: String?): String? =
    message?.substringBefore("Headers:")?.trim()

data class LanguageMenuUiState(
    val downloadableLanguages: List<DownloadableLanguage> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
)

/** Publishing (pushing local content up as global admin material) no longer happens from
 * Android at all — that's exclusively a job for the admin web panel (admin-web/) now. This
 * ViewModel only ever reads: browsing/downloading/refreshing admin-published content, same as
 * any other user would. */
@HiltViewModel
class LanguageMenuViewModel @Inject constructor(
    private val languageRepository: LanguageRepository,
    private val prefs: UserPreferences,
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
        viewModelScope.launch { refreshDownloadable() }
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

    /** Anyone with a `remoteId`-linked language can pull later server-side changes into their
     * existing local copy — see [ContentSyncRepository.refreshLanguage] for what this does and
     * doesn't touch. */
    fun refresh(languageId: Long) {
        if (_uiState.value.busy) return
        _uiState.value = _uiState.value.copy(busy = true, message = null)
        viewModelScope.launch {
            val result = runCatching { syncRepository.refreshLanguage(languageId) }
            _uiState.value = _uiState.value.copy(
                busy = false,
                message = if (result.isSuccess) "Оновлено" else "Не вдалося оновити: ${sanitizeSyncError(result.exceptionOrNull()?.message)}",
            )
        }
    }

    /** Downloads an admin-published language into this profile's own local copy, then opens it. */
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
