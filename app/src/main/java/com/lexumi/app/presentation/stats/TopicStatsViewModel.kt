package com.lexumi.app.presentation.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.repository.WordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.roundToInt

data class WordStatRow(
    val term: String,
    val translation: String,
    val timesSeen: Int,
    val totalCorrect: Int,
    val bestStreak: Int,
    val accuracyPercent: Int,
)

@HiltViewModel
class TopicStatsViewModel @Inject constructor(
    wordRepository: WordRepository,
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
                    WordStatRow(
                        term = word.term,
                        translation = word.translation,
                        timesSeen = word.timesSeen,
                        totalCorrect = word.totalCorrect,
                        bestStreak = word.bestStreak,
                        accuracyPercent = accuracy,
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
