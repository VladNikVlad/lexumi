package com.lexumi.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.data.auth.AuthRepository
import com.lexumi.app.data.datastore.UserPreferences
import com.lexumi.app.data.local.LexumiDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val database: LexumiDatabase,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val wordsPerSession: StateFlow<Int> = prefs.wordsPerSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val repetitions: StateFlow<Int> = prefs.repetitions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val remindersEnabled: StateFlow<Boolean> = prefs.remindersEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut

    private val _dataCleared = MutableStateFlow(false)
    val dataCleared: StateFlow<Boolean> = _dataCleared

    fun setWordsPerSession(count: Int) = viewModelScope.launch { prefs.setWordsPerSession(count) }
    fun setRepetitions(count: Int) = viewModelScope.launch { prefs.setRepetitions(count) }
    fun setRemindersEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setRemindersEnabled(enabled) }

    /** "Вийти" — signs out of the Google/Supabase session too, not just the local profile —
     * app returns all the way to the sign-in screen, not just the local profile picker. */
    fun logout() = viewModelScope.launch {
        prefs.clearCurrentProfile()
        authRepository.signOut()
        _loggedOut.value = true
    }

    /** "Видалити всі дані" — wipes the whole local database (all languages, content, progress). */
    fun deleteAllData() = viewModelScope.launch {
        database.clearAllTables()
        prefs.clearCurrentProfile()
        _dataCleared.value = true
    }
}
