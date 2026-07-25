package com.lexumi.app.presentation.addcontent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.model.Rule
import com.lexumi.app.domain.repository.SectionRepository
import com.lexumi.app.domain.repository.TopicRepository
import com.lexumi.app.domain.repository.RuleRepository
import com.lexumi.app.domain.usecase.AddResult
import com.lexumi.app.domain.usecase.AddWordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

const val MAX_WORD_IMAGE_BYTES = 100 * 1024 // 100kb, per point 8

@HiltViewModel
class AddWordViewModel @Inject constructor(
    private val addWord: AddWordUseCase,
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
            val section = topic?.let { sectionRepository.getSection(it.sectionId) }
            _languageId.value = section?.languageId
        }
    }

    fun submit(imagePath: String?, term: String, translation: String, ruleId: Long?) {
        viewModelScope.launch {
            when (val result = addWord(topicId, imagePath, term, translation, ruleId)) {
                is AddResult.Success -> _created.value = true
                AddResult.AlreadyExists -> _error.value = "Таке слово вже є в цій темі"
                AddResult.Blank -> _error.value = "Заповніть слово і переклад"
            }
        }
    }

    fun clearError() { _error.value = null }
}
