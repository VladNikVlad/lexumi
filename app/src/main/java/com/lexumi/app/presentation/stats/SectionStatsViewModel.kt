package com.lexumi.app.presentation.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.model.Word
import com.lexumi.app.domain.repository.TopicRepository
import com.lexumi.app.domain.repository.WordRepository
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

/** A word counts as "learned" once it has moved past the very first (multiple-choice) rating. */
private fun isLearned(word: Word): Boolean = word.rating >= 1

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
