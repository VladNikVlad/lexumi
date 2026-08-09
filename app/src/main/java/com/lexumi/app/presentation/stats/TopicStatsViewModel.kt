package com.lexumi.app.presentation.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.repository.SentenceRepository
import com.lexumi.app.domain.repository.WordRepository
import com.lexumi.app.domain.usecase.SubmitSentenceAnswerUseCase
import com.lexumi.app.domain.usecase.SubmitWordAnswerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class WordStatRow(
    val id: Long,
    val term: String,
    val translation: String,
    val rating: Int,
    val timesSeen: Int,
    val totalCorrect: Int,
    val bestStreak: Int,
    val accuracyPercent: Int,
)

data class SentenceStatRow(
    val id: Long,
    val text: String,
    val translation: String,
    val rating: Int,
    val timesSeen: Int,
    val totalCorrect: Int,
    val bestStreak: Int,
    val accuracyPercent: Int,
)

@HiltViewModel
class TopicStatsViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val sentenceRepository: SentenceRepository,
    private val submitWordAnswer: SubmitWordAnswerUseCase,
    private val submitSentenceAnswer: SubmitSentenceAnswerUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val topicId: Long = checkNotNull(savedStateHandle["topicId"])

    val rows: StateFlow<List<WordStatRow>> = wordRepository.observeWords(topicId)
        .map { words ->
            words
                .filter { it.timesSeen > 0 }
                .sortedByDescending { it.timesSeen }
                .map { word ->
                    val accuracy = if (word.timesSeen == 0) 0 else (word.totalCorrect * 100.0 / word.timesSeen).roundToInt()
                    WordStatRow(word.id, word.term, word.translation, word.rating, word.timesSeen, word.totalCorrect, word.bestStreak, accuracy)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sentenceRows: StateFlow<List<SentenceStatRow>> = sentenceRepository.observeSentences(topicId)
        .map { sentences ->
            sentences
                .filter { it.timesSeen > 0 }
                .sortedByDescending { it.timesSeen }
                .map { s ->
                    val accuracy = if (s.timesSeen == 0) 0 else (s.totalCorrect * 100.0 / s.timesSeen).roundToInt()
                    SentenceStatRow(s.id, s.text, s.translations.firstOrNull().orEmpty(), s.rating, s.timesSeen, s.totalCorrect, s.bestStreak, accuracy)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Tapping a mastered (rating 4) word and confirming brings it back to the cards round (rating 2). */
    fun repeatWord(wordId: Long) {
        viewModelScope.launch {
            val word = wordRepository.getWord(wordId) ?: return@launch
            if (word.rating == 4) submitWordAnswer.repeatMasteredWord(word)
        }
    }

    /** Same idea for a mastered sentence. */
    fun repeatSentence(sentenceId: Long) {
        viewModelScope.launch {
            val sentence = sentenceRepository.getSentences(topicId).firstOrNull { it.id == sentenceId } ?: return@launch
            if (sentence.rating == 4) submitSentenceAnswer.repeatMastered(sentence)
        }
    }
}
