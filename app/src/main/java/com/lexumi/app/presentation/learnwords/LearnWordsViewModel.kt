package com.lexumi.app.presentation.learnwords

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.data.datastore.UserPreferences
import com.lexumi.app.domain.model.AnswerCheck
import com.lexumi.app.domain.model.Rule
import com.lexumi.app.domain.model.Word
import com.lexumi.app.domain.repository.LanguageRepository
import com.lexumi.app.domain.repository.RuleRepository
import com.lexumi.app.domain.repository.SectionRepository
import com.lexumi.app.domain.repository.TopicRepository
import com.lexumi.app.domain.repository.WordRepository
import com.lexumi.app.domain.usecase.*
import com.lexumi.app.util.TtsManager
import com.lexumi.app.util.SoundFeedbackPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WordPrompt(
    val word: Word,
    val askTermFirst: Boolean,
    val choices: List<String>?, // null when the word is at level 1 (typed answer)
)

sealed class WordFeedback {
    data object None : WordFeedback()
    data object Correct : WordFeedback()
    data class OneLetterTypo(val correctSpelling: String) : WordFeedback()
    data class Wrong(val correctSpelling: String) : WordFeedback()
}

data class LearnWordsUiState(
    val loading: Boolean = true,
    val prompt: WordPrompt? = null,
    val feedback: WordFeedback = WordFeedback.None,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val sessionDone: Boolean = false,
    val editError: String? = null,
)

@HiltViewModel
class LearnWordsViewModel @Inject constructor(
    private val getSessionWords: GetSessionWordsUseCase,
    private val buildMultipleChoice: BuildMultipleChoiceUseCase,
    private val submitAnswer: SubmitWordAnswerUseCase,
    private val editWord: EditWordUseCase,
    private val deleteWord: DeleteWordUseCase,
    private val wordRepository: WordRepository,
    private val topicRepository: TopicRepository,
    private val sectionRepository: SectionRepository,
    private val languageRepository: LanguageRepository,
    ruleRepository: RuleRepository,
    private val prefs: UserPreferences,
    private val ttsManager: TtsManager,
    private val soundFeedbackPlayer: SoundFeedbackPlayer,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val topicId: Long = checkNotNull(savedStateHandle["topicId"])

    private val _uiState = MutableStateFlow(LearnWordsUiState())
    val uiState: StateFlow<LearnWordsUiState> = _uiState

    private val _languageId = MutableStateFlow<Long?>(null)
    val rules: StateFlow<List<Rule>> = _languageId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else ruleRepository.observeRules(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** The language's auto-assigned TTS voice, so words are read aloud in a natural voice for that language. */
    private val _voiceName = MutableStateFlow<String?>(null)
    val voiceName: StateFlow<String?> = _voiceName

    private var queue: MutableList<Long> = mutableListOf()

    init {
        viewModelScope.launch {
            prefs.setLastSession(topicId, "learn_words")
            val wordsPerSession = prefs.wordsPerSession.first()
            val repetitions = prefs.repetitions.first()
            queue = getSessionWords(topicId, wordsPerSession, repetitions).toMutableList()
            _uiState.value = _uiState.value.copy(totalCount = queue.size, loading = false)
            advance()

            val topic = topicRepository.getTopic(topicId)
            val languageId = topic?.let { sectionRepository.getSection(it.sectionId)?.languageId }
            _languageId.value = languageId
            _voiceName.value = languageId?.let { languageRepository.getLanguage(it)?.voiceName }
        }
    }

    private suspend fun advance() {
        if (queue.isEmpty()) {
            prefs.clearLastSession()
            _uiState.value = _uiState.value.copy(sessionDone = true, prompt = null)
            return
        }
        // Re-fetch the word fresh each time: an earlier repeat in this same
        // session may have changed its level/score, and the prompt should
        // reflect that current state, not a stale snapshot from the start.
        val wordId = queue.removeAt(0)
        val word = wordRepository.getWord(wordId) ?: run { advance(); return }
        val askTermFirst = word.askTermFirst()
        val choices = if (word.level == 0) buildMultipleChoice(word, askTermFirst) else null
        _uiState.value = _uiState.value.copy(
            prompt = WordPrompt(word, askTermFirst, choices),
            feedback = WordFeedback.None,
        )
    }

    fun submitChoice(chosenText: String) {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch {
            val correctText = if (prompt.askTermFirst) prompt.word.translation else prompt.word.term
            val wasCorrect = chosenText == correctText
            submitAnswer.submitChoice(prompt.word, wasCorrect)
            if (wasCorrect) soundFeedbackPlayer.playCorrect() else soundFeedbackPlayer.playWrong()
            _uiState.value = _uiState.value.copy(
                feedback = if (wasCorrect) WordFeedback.Correct else WordFeedback.Wrong(correctText),
                completedCount = _uiState.value.completedCount + 1,
            )
        }
    }

    fun submitTyped(userInput: String) {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch {
            val expected = if (prompt.askTermFirst) prompt.word.translation else prompt.word.term
            val (_, check) = submitAnswer.submitTypedAnswer(prompt.word, userInput, expected)
            if (check is AnswerCheck.Wrong) soundFeedbackPlayer.playWrong() else soundFeedbackPlayer.playCorrect()
            val feedback = when (check) {
                is AnswerCheck.Correct -> WordFeedback.Correct
                is AnswerCheck.OneLetterTypo -> WordFeedback.OneLetterTypo(check.correctSpelling)
                is AnswerCheck.Wrong -> WordFeedback.Wrong(check.correctSpelling)
            }
            _uiState.value = _uiState.value.copy(feedback = feedback, completedCount = _uiState.value.completedCount + 1)
        }
    }

    /** "Вже знаю" — marks the word mastered right away and skips it, removing any other queued repeats of it too. */
    fun markCurrentAsKnown() {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch {
            val updated = prompt.word.copy(score = SCORE_TO_MASTER, level = 1)
            wordRepository.updateWord(updated)
            queue.removeAll { it == prompt.word.id }
            advance()
        }
    }

    fun addCurrentToReview() {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch {
            val updated = submitAnswer.toggleReviewList(prompt.word, true)
            // Keep the in-memory prompt in sync, or the next answer submit
            // would overwrite this flag back to false using a stale copy.
            if (_uiState.value.prompt?.word?.id == updated.id) {
                _uiState.value = _uiState.value.copy(prompt = _uiState.value.prompt!!.copy(word = updated))
            }
        }
    }

    /** Saves edits (text and/or picture) to the word currently on screen without losing its learning progress. */
    fun editCurrentWord(term: String, translation: String, imagePath: String?, ruleId: Long?) {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch {
            when (val result = editWord(prompt.word, term, translation, imagePath, ruleId)) {
                is AddResult.Success -> {
                    val updated = prompt.word.copy(term = term.trim(), translation = translation.trim(), imagePath = imagePath, ruleId = ruleId)
                    _uiState.value = _uiState.value.copy(prompt = prompt.copy(word = updated), editError = null)
                }
                AddResult.AlreadyExists -> _uiState.value = _uiState.value.copy(editError = "Таке слово вже є в цій темі")
                AddResult.Blank -> _uiState.value = _uiState.value.copy(editError = "Заповніть слово і переклад")
            }
        }
    }

    /** Deletes the word currently on screen (and any other queued repeats of it) and moves on. */
    fun deleteCurrentWord() {
        val prompt = _uiState.value.prompt ?: return
        viewModelScope.launch {
            queue.removeAll { it == prompt.word.id }
            deleteWord(prompt.word)
            advance()
        }
    }

    fun clearEditError() { _uiState.value = _uiState.value.copy(editError = null) }

    fun speak(text: String) { ttsManager.speak(text, _voiceName.value) }

    fun next() {
        viewModelScope.launch { advance() }
    }
}
