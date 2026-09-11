package com.lexumi.app.presentation.profile

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.data.datastore.UserPreferences
import com.lexumi.app.domain.model.UserProfile
import com.lexumi.app.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Admin status doesn't affect anything in the Android app anymore — publishing/editing admin
 * content is a web-panel-only job now (admin-web/), so there's nothing here to check it for. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    val currentProfileId: StateFlow<Long?> = prefs.currentProfileId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val profiles: StateFlow<List<UserProfile>> = profileRepository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setAppLanguage(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }

    fun currentAppLanguageTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) "uk" else locales[0]?.language ?: "uk"
    }

    fun renameProfile(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch { profileRepository.renameProfile(id, trimmed) }
    }

    /** Creates an additional local profile and switches to it — replaces the old "Змінити користувача" flow. */
    fun createAndSwitchToNewProfile(name: String) = viewModelScope.launch {
        val finalName = name.trim().ifBlank { profileRepository.nextDefaultProfileName() }
        val id = profileRepository.createProfile(finalName)
        prefs.clearSelectedLanguage()
        prefs.clearLastSession()
        prefs.setCurrentProfile(id)
    }
}
