package com.lexumi.app.presentation.section

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.model.Section
import com.lexumi.app.domain.repository.SectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SectionsViewModel @Inject constructor(
    sectionRepository: SectionRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val languageId: Long = checkNotNull(savedStateHandle["languageId"])

    /** true = "Самостійне вивчення" (admin-synced, always read-only), false = "Власний матеріал"
     * (the user's own, always editable) — see Screen.Sections' own doc comment. */
    private val adminMode: Boolean = checkNotNull(savedStateHandle["adminMode"])

    val canEdit: Boolean = !adminMode

    val sections: StateFlow<List<Section>> = (
        if (adminMode) sectionRepository.observeAdminSections(languageId)
        else sectionRepository.observePersonalSections(languageId)
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
