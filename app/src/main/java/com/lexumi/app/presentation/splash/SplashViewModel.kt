package com.lexumi.app.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.data.auth.AuthRepository
import com.lexumi.app.data.datastore.UserPreferences
import com.lexumi.app.domain.repository.LanguageRepository
import com.lexumi.app.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashDestination {
    data object Loading : SplashDestination()
    data object SignIn : SplashDestination()        // no Supabase session yet -> Google sign-in
    data object LanguageMenu : SplashDestination()  // signed in, no language chosen yet
    data class Home(val languageId: Long) : SplashDestination() // returning user
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val profileRepository: ProfileRepository,
    private val languageRepository: LanguageRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination

    init {
        viewModelScope.launch {
            // simulated asset-loading progress bar (point 1 of the scenario)
            for (i in 1..20) {
                _progress.value = i / 20f
                kotlinx.coroutines.delay(40)
            }

            if (!authRepository.isSignedIn()) {
                _destination.value = SplashDestination.SignIn
                return@launch
            }
            val user = authRepository.currentUser()
            if (user == null) {
                // Shouldn't happen if isSignedIn() just returned true, but the local session
                // could theoretically vanish between the two checks — fail safe to sign-in.
                _destination.value = SplashDestination.SignIn
                return@launch
            }

            // Exactly one local profile per signed-in account — created on this account's very
            // first sign-in (see ProfileRepository doc). Replaces the old free-for-all where a
            // cleared `currentProfileId` just fell back to "whichever local profile happens to
            // exist", which is how a second Google account on the same device used to end up
            // seeing the first account's languages/progress/name.
            val profileId = profileRepository.getOrCreateForAuthUser(user.id, user.displayName)
            prefs.setCurrentProfile(profileId)

            val storedLanguageId = prefs.selectedLanguageId.first()
            val language = storedLanguageId?.let { languageRepository.getLanguage(it) }
            // Not enough that the language still exists — it could belong to a DIFFERENT
            // account's profile that was previously signed in on this device (selectedLanguageId
            // itself isn't cleared on sign-out, only currentProfileId is).
            val languageId = language?.takeIf { it.profileId == profileId }?.id
            if (storedLanguageId != null && languageId == null) {
                prefs.clearSelectedLanguage()
                prefs.clearLastSession()
            }
            _destination.value = if (languageId == null) SplashDestination.LanguageMenu else SplashDestination.Home(languageId)
        }
    }
}
