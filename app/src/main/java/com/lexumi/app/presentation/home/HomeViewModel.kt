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
     * which never needs a network call). Errors are swallowed; this isn't a user-facing action —
     * the "Оновити" button on LanguageMenuScreen stays available for a deliberate retry. */
    fun enterSelfStudy() {
        viewModelScope.launch {
            if (connectivityChecker.isOnline()) {
                runCatching { syncRepository.refreshLanguage(languageId) }
            }
        }
    }
}
