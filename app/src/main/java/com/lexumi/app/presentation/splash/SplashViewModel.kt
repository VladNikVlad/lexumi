package com.lexumi.app.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.data.datastore.UserPreferences
import com.lexumi.app.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashDestination {
    data object Loading : SplashDestination()
    data object Welcome : SplashDestination()       // no profile yet -> create one
    data object LanguageMenu : SplashDestination()  // profile exists, no language chosen yet
    data class Home(val languageId: Long) : SplashDestination() // returning user
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val profileRepository: ProfileRepository,
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
            val profileId = prefs.currentProfileId.first()
            val hasProfiles = profileRepository.profileCount() > 0
            _destination.value = when {
                profileId == null && !hasProfiles -> SplashDestination.Welcome
                profileId == null -> SplashDestination.LanguageMenu
                else -> {
                    val languageId = prefs.selectedLanguageId.first()
                    if (languageId == null) SplashDestination.LanguageMenu
                    else SplashDestination.Home(languageId)
                }
            }
        }
    }
}
