package com.lexumi.app.presentation.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.data.datastore.LastSession
import com.lexumi.app.data.datastore.UserPreferences
import com.lexumi.app.data.network.ConnectivityChecker
import com.lexumi.app.data.sync.ContentSyncRepository
import com.lexumi.app.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Postgrest/ktor exception messages can include the full failed HTTP request for debugging —
 * headers and all, which means a live `Authorization: Bearer <token>` ends up in this string.
 * Never show that in a user-facing error message; cut it off at the first header dump. Mirrors
 * the identical helper in LanguageMenuViewModel.kt. */
private fun sanitizeSyncError(message: String?): String? =
    message?.substringBefore("Headers:")?.trim()

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val topicRepository: TopicRepository,
    private val syncRepository: ContentSyncRepository,
    private val connectivityChecker: ConnectivityChecker,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val languageId: Long = checkNotNull(savedStateHandle["languageId"])

    private val _lastSession = MutableStateFlow<LastSession?>(null)
    val lastSession: StateFlow<LastSession?> = _lastSession

    // Was silently swallowed before (runCatching with no result handling) — a failed background
    // refresh looked identical to a successful one, so a stale topic just stayed stale with no
    // clue why. Surfaced here so the screen can show it instead.
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    init {
        viewModelScope.launch {
            prefs.lastSession.collectLatest { session ->
                // A saved "continue" session can point at a topic that no
                // longer exists (e.g. after a dev-time database reset) —
                // verify it before offering "Продовжити навчання".
                if (session != null && topicRepository.getTopic(session.topicId) == null) {
                    prefs.clearLastSession()
                    _lastSession.value = null
                } else {
                    _lastSession.value = session
                }
            }
        }
    }

    /** Best-effort background refresh, triggered only when the user actually heads into
     * "Самостійне вивчення" — not at language selection, since picking a language doesn't imply
     * the user wants server content at all (they might only be here for "Власний матеріал",
     * which never needs a network call). Doesn't block navigation — the mode screen opens
     * immediately and picks up whatever this refresh changes as it completes (both screens read
     * the same Room tables as Flows). A failure is surfaced via [syncError] rather than swallowed,
     * so a stuck stale topic has a visible reason instead of silently never updating. */
    fun enterSelfStudy() {
        viewModelScope.launch {
            _syncError.value = null
            if (connectivityChecker.isOnline()) {
                val result = runCatching { syncRepository.refreshLanguage(languageId) }
                if (result.isFailure) {
                    _syncError.value = "Не вдалося оновити: ${sanitizeSyncError(result.exceptionOrNull()?.message)}"
                }
            }
        }
    }
}
