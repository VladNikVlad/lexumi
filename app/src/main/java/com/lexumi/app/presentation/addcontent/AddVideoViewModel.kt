package com.lexumi.app.presentation.addcontent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.model.Rule
import com.lexumi.app.domain.model.QuestionAnswerType
import com.lexumi.app.domain.model.TestQuestion
import com.lexumi.app.domain.repository.RuleRepository
import com.lexumi.app.domain.repository.SectionRepository
import com.lexumi.app.domain.repository.TopicRepository
import com.lexumi.app.domain.usecase.AddResult
import com.lexumi.app.domain.usecase.AddVideoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddVideoViewModel @Inject constructor(
    private val addVideo: AddVideoUseCase,
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

    fun submit(
        name: String, youtubeUrl: String, originalText: String?, translationText: String?,
        ruleIds: List<Long>, questions: List<Pair<String, Boolean>>,
    ) {
        viewModelScope.launch {
            val testQuestions = questions.map {
                TestQuestion(0, it.first, QuestionAnswerType.TRUE_FALSE, it.second, emptyList())
            }
            when (addVideo(topicId, name, youtubeUrl, originalText, translationText, ruleIds, testQuestions)) {
                is AddResult.Success -> _created.value = true
                AddResult.AlreadyExists -> _error.value = "Таке відео вже є в цій темі"
                AddResult.Blank -> _error.value = "Введіть назву і посилання"
            }
        }
    }

    fun clearError() { _error.value = null }
}
