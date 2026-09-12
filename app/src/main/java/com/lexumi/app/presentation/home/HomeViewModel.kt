package com.lexumi.app.presentation.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.data.datastore.LastSession
import com.lexumi.app.data.datastore.UserPreferences
import com.lexumi.app.data.network.ConnectivityChecker
import com.lexumi.app.data.sync.ContentSyncRepository
import com.lexumi.app.domain.repository.LanguageRepository
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
    private val languageRepository: LanguageRepository,
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
        viewModelScope.launch { refreshSelfStudyContent() }
    }

    /** Best-effort background refresh of "Самостійне вивчення" content, run once whenever Home is
     * reached for this language — for a returning user Splash navigates straight here
     * (SplashViewModel.Home), so this is effectively "on app open", not something tied to which
     * button the user happens to tap next. Previously this only ran from the "Самостійне
     * вивчення" button's onClick, which meant "Продовжити навчання" (the button actually shown
     * whenever a saved session exists — i.e. on every repeat visit) skipped it entirely, so a
     * server-side deletion never got picked up during normal use. Doesn't block navigation — both
     * mode screens read the same Room tables as Flows, so whatever this changes just shows up as
     * it completes. Skipped entirely for a language that was never linked to the server
     * (`remoteId == null`, e.g. a language created purely for "Власний матеріал") — otherwise
     * every such user would see [syncError] on every launch for a call that can never succeed. A
     * real failure is surfaced via [syncError] rather than swallowed, so a stuck stale topic has a
     * visible reason instead of silently never updating — but nothing is shown while this just
     * works, per design: the user should never need to notice an update is happening. */
    private suspend fun refreshSelfStudyContent() {
        val language = languageRepository.getLanguage(languageId)
        if (language?.remoteId == null) return
        if (!connectivityChecker.isOnline()) return
        val result = runCatching { syncRepository.refreshLanguage(languageId) }
        if (result.isFailure) {
            _syncError.value = "Не вдалося оновити: ${sanitizeSyncError(result.exceptionOrNull()?.message)}"
        }
    }
}
