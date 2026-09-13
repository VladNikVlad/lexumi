package com.lexumi.app.presentation.profile

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.data.datastore.UserPreferences
import com.lexumi.app.domain.model.UserProfile
import com.lexumi.app.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Admin status doesn't affect anything in the Android app anymore — publishing/editing admin
 * content is a web-panel-only job now (admin-web/), so there's nothing here to check it for.
 * There's also no more "switch to a different local profile" here — one signed-in account is
 * exactly one profile now (see ProfileRepository); changing "who's using the app" means signing
 * out and signing in with a different Google account instead. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    val currentProfileId: StateFlow<Long?> = prefs.currentProfileId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentProfile: StateFlow<UserProfile?> = currentProfileId.flatMapLatest { id ->
        if (id == null) flowOf(null) else profileRepository.observeProfile(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
}
