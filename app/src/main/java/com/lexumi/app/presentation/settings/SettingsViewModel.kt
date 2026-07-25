package com.lexumi.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.data.datastore.UserPreferences
import com.lexumi.app.data.local.LexumiDatabase
import com.lexumi.app.domain.model.UserProfile
import com.lexumi.app.domain.repository.ProfileRepository
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
    private val profileRepository: ProfileRepository,
    private val database: LexumiDatabase,
) : ViewModel() {

    val wordsPerSession: StateFlow<Int> = prefs.wordsPerSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val repetitions: StateFlow<Int> = prefs.repetitions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val remindersEnabled: StateFlow<Boolean> = prefs.remindersEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val currentProfileId: StateFlow<Long?> = prefs.currentProfileId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val profiles: StateFlow<List<UserProfile>> = profileRepository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut

    private val _dataCleared = MutableStateFlow(false)
    val dataCleared: StateFlow<Boolean> = _dataCleared

    fun setWordsPerSession(count: Int) = viewModelScope.launch { prefs.setWordsPerSession(count) }
    fun setRepetitions(count: Int) = viewModelScope.launch { prefs.setRepetitions(count) }
    fun setRemindersEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setRemindersEnabled(enabled) }

    /** "Змінити користувача" — switches the active local profile without deleting anyone's data. */
    fun switchToProfile(profileId: Long) = viewModelScope.launch {
        prefs.clearSelectedLanguage()
        prefs.clearLastSession()
        prefs.setCurrentProfile(profileId)
    }

    fun createAndSwitchToNewProfile(name: String) = viewModelScope.launch {
        val id = profileRepository.createProfile(name.ifBlank { "Новий профіль" })
        switchToProfile(id)
    }

    /** "Вийти" — clears the active profile session; app returns to the welcome/profile picker. */
    fun logout() = viewModelScope.launch {
        prefs.clearCurrentProfile()
        _loggedOut.value = true
    }

    /** "Видалити всі дані" — wipes the whole local database (all languages, content, progress). */
    fun deleteAllData() = viewModelScope.launch {
        database.clearAllTables()
        prefs.clearCurrentProfile()
        _dataCleared.value = true
    }
}
