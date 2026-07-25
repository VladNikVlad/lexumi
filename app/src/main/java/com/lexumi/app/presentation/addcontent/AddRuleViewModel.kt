package com.lexumi.app.presentation.addcontent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.repository.SectionRepository
import com.lexumi.app.domain.repository.TopicRepository
import com.lexumi.app.domain.usecase.AddResult
import com.lexumi.app.domain.usecase.AddRuleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddRuleViewModel @Inject constructor(
    private val addRule: AddRuleUseCase,
    private val topicRepository: TopicRepository,
    private val sectionRepository: SectionRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val topicId: Long = checkNotNull(savedStateHandle["topicId"])
    private var languageId: Long? = null

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _created = MutableStateFlow(false)
    val created: StateFlow<Boolean> = _created

    init {
        viewModelScope.launch {
            val topic = topicRepository.getTopic(topicId)
            languageId = topic?.let { sectionRepository.getSection(it.sectionId)?.languageId }
        }
    }

    fun submit(name: String, text: String) {
        viewModelScope.launch {
            val langId = languageId ?: return@launch
            when (addRule(langId, name, text)) {
                is AddResult.Success -> _created.value = true
                AddResult.AlreadyExists -> _error.value = "Таке правило вже існує в цій мові"
                AddResult.Blank -> _error.value = "Заповніть назву і текст правила"
            }
        }
    }

    fun clearError() { _error.value = null }
}
