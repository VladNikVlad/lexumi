package com.lexumi.app.presentation.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.data.datastore.UserPreferences
import com.lexumi.app.domain.model.Language
import com.lexumi.app.domain.repository.LanguageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageMenuViewModel @Inject constructor(
    private val languageRepository: LanguageRepository,
    private val prefs: UserPreferences,
) : ViewModel() {

    val languages: StateFlow<List<Language>> = prefs.currentProfileId
        .flatMapLatest { profileId ->
            if (profileId == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else languageRepository.observeLanguages(profileId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selected = MutableStateFlow<Long?>(null)
    val selected: StateFlow<Long?> = _selected

    fun selectLanguage(languageId: Long) {
        viewModelScope.launch {
            prefs.setSelectedLanguage(languageId)
            _selected.value = languageId
        }
    }
}
