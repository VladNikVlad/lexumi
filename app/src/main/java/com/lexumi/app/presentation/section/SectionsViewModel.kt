package com.lexumi.app.presentation.section

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.model.Section
import com.lexumi.app.domain.repository.SectionRepository
import com.lexumi.app.domain.usecase.IsLanguageEditableUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SectionsViewModel @Inject constructor(
    sectionRepository: SectionRepository,
    isLanguageEditable: IsLanguageEditableUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val languageId: Long = checkNotNull(savedStateHandle["languageId"])

    val sections: StateFlow<List<Section>> = sectionRepository.observeSections(languageId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _canEdit = MutableStateFlow(true)
    val canEdit: StateFlow<Boolean> = _canEdit

    init {
        viewModelScope.launch { _canEdit.value = isLanguageEditable(languageId) }
    }
}
