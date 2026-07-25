package com.lexumi.app.presentation.audio

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.model.AudioDialog
import com.lexumi.app.domain.model.Rule
import com.lexumi.app.domain.model.TestQuestion
import com.lexumi.app.domain.repository.AudioDialogRepository
import com.lexumi.app.domain.repository.RuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioListViewModel @Inject constructor(
    audioDialogRepository: AudioDialogRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val topicId: Long = checkNotNull(savedStateHandle["topicId"])
    val dialogs: StateFlow<List<AudioDialog>> = audioDialogRepository.observeDialogs(topicId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

data class AudioPlayerUiState(
    val dialog: AudioDialog? = null,
    val questions: List<TestQuestion> = emptyList(),
    val rules: List<Rule> = emptyList(),
    val showTranslation: Boolean = false,
    val isPlaying: Boolean = false,
)

@HiltViewModel
class AudioPlayerViewModel @Inject constructor(
    private val audioDialogRepository: AudioDialogRepository,
    private val ruleRepository: RuleRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val dialogId: Long = checkNotNull(savedStateHandle["dialogId"])

    private val _uiState = MutableStateFlow(AudioPlayerUiState())
    val uiState: StateFlow<AudioPlayerUiState> = _uiState

    init {
        viewModelScope.launch {
            val dialog = audioDialogRepository.getDialog(dialogId)
            val questions = audioDialogRepository.getQuestions(dialogId)
            val rules = dialog?.let { ruleRepository.getRulesByIds(it.ruleIds) } ?: emptyList()
            _uiState.value = AudioPlayerUiState(dialog, questions, rules)
        }
    }

    fun toggleTranslation() { _uiState.value = _uiState.value.copy(showTranslation = !_uiState.value.showTranslation) }
    fun setPlaying(playing: Boolean) { _uiState.value = _uiState.value.copy(isPlaying = playing) }
}
