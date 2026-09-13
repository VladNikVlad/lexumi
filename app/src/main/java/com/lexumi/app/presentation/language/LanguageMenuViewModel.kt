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

    /** Only meaningful for a language the user created themselves — renaming an admin-downloaded
     * one would just get overwritten by the next background structure sync. */
    fun renameLanguage(languageId: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch { languageRepository.renameLanguage(languageId, trimmed) }
    }

    /** If this was the currently selected language, clears that too — otherwise Splash would try
     * to reopen a language that no longer exists (it does self-heal there, but no need to rely on
     * that when we already know here that it's gone). */
    fun deleteLanguage(languageId: Long) {
        viewModelScope.launch {
            if (prefs.selectedLanguageId.first() == languageId) {
                prefs.clearSelectedLanguage()
                prefs.clearLastSession()
            }
            languageRepository.deleteLanguage(languageId)
            refreshDownloadable()
        }
    }

    /** Downloads an admin-published language into this profile's own local copy, then opens it.
     * Pulling in LATER changes to an already-downloaded language is no longer a manual action
     * here — HomeViewModel does it silently in the background as soon as the language is opened. */
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
