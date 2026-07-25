package com.lexumi.app.presentation.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.data.datastore.UserPreferences
import com.lexumi.app.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val prefs: UserPreferences,
) : ViewModel() {

    private val _created = MutableStateFlow(false)
    val created: StateFlow<Boolean> = _created

    fun createProfile(name: String) {
        val trimmed = name.trim().ifBlank { "Мій профіль" }
        viewModelScope.launch {
            val id = profileRepository.createProfile(trimmed)
            prefs.setCurrentProfile(id)
            _created.value = true
        }
    }
}
