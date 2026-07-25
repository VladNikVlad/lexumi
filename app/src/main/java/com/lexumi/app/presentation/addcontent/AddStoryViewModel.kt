package com.lexumi.app.presentation.addcontent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.model.Rule
import com.lexumi.app.domain.repository.RuleRepository
import com.lexumi.app.domain.repository.SectionRepository
import com.lexumi.app.domain.repository.TopicRepository
import com.lexumi.app.domain.usecase.AddResult
import com.lexumi.app.domain.usecase.AddStoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddStoryViewModel @Inject constructor(
    private val addStory: AddStoryUseCase,
    private val topicRepository: TopicRepository,
    private val sectionRepository: SectionRepository,
    ruleRepository: RuleRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val topicId: Long = checkNotNull(savedStateHandle["topicId"])
    private val _languageId = MutableStateFlow<Long?>(null)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _created = MutableStateFlow(false)
    val created: StateFlow<Boolean> = _created

    val rules: StateFlow<List<Rule>> = _languageId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else ruleRepository.observeRules(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val topic = topicRepository.getTopic(topicId)
            _languageId.value = topic?.let { sectionRepository.getSection(it.sectionId)?.languageId }
        }
    }

    fun submit(name: String, text: String, translation: String?, ruleIds: List<Long>) {
        viewModelScope.launch {
            when (addStory(topicId, name, text, translation, ruleIds)) {
                is AddResult.Success -> _created.value = true
                AddResult.AlreadyExists -> _error.value = "Така історія вже є в цій темі"
                AddResult.Blank -> _error.value = "Заповніть назву і текст історії"
            }
        }
    }

    fun clearError() { _error.value = null }
}
