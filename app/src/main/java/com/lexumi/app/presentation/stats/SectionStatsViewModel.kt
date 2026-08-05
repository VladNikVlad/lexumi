package com.lexumi.app.presentation.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.model.Word
import com.lexumi.app.domain.repository.TopicRepository
import com.lexumi.app.domain.repository.WordRepository
import com.lexumi.app.domain.usecase.SCORE_TO_MASTER
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SectionWordStats(
    val uniqueWordCount: Int = 0,
    val learnedCount: Int = 0,
)

/**
 * A word counts as "learned" once it's answered correctly at least 80% of
 * the time after being repeated 10+ times, or the user tapped "Вже знаю" on
 * it — either way it must have progressed past the very first level.
 */
private fun isLearned(word: Word): Boolean {
    if (word.level < 1) return false
    if (word.score >= SCORE_TO_MASTER) return true // reached via practice, or set directly by "Вже знаю"
    if (word.timesSeen < 10) return false
    val accuracy = word.totalCorrect * 100.0 / word.timesSeen
    return accuracy >= 80.0
}

@HiltViewModel
class SectionStatsViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val wordRepository: WordRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val sectionId: Long = checkNotNull(savedStateHandle["sectionId"])

    private val _stats = MutableStateFlow(SectionWordStats())
    val stats: StateFlow<SectionWordStats> = _stats

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    init {
        viewModelScope.launch {
            topicRepository.observeTopics(sectionId).collectLatest { topics ->
                val allWords = topics.flatMap { wordRepository.getWords(it.id) }
                _stats.value = SectionWordStats(
                    uniqueWordCount = allWords.size,
                    learnedCount = allWords.count { isLearned(it) },
                )
                _loading.value = false
            }
        }
    }
}
