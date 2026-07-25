package com.lexumi.app.presentation.topicaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TopicActionAvailability(
    val hasWords: Boolean = false,
    val hasVideos: Boolean = false,
    val hasAudio: Boolean = false,
    val hasSentences: Boolean = false,
    val hasStories: Boolean = false,
    val hasImages: Boolean = false,
)

@HiltViewModel
class TopicActionViewModel @Inject constructor(
    wordRepository: WordRepository,
    videoRepository: VideoRepository,
    audioDialogRepository: AudioDialogRepository,
    sentenceRepository: SentenceRepository,
    storyRepository: StoryRepository,
    imageContentRepository: ImageContentRepository,
    topicRepository: TopicRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val topicId: Long = checkNotNull(savedStateHandle["topicId"])

    private val _topicName = MutableStateFlow("")
    val topicName: StateFlow<String> = _topicName

    init {
        viewModelScope.launch {
            _topicName.value = topicRepository.getTopic(topicId)?.name.orEmpty()
        }
    }

    val availability: StateFlow<TopicActionAvailability> = combine(
        combine(
            wordRepository.observeWords(topicId),
            videoRepository.observeVideos(topicId),
            audioDialogRepository.observeDialogs(topicId),
        ) { words, videos, audio -> Triple(words.isNotEmpty(), videos.isNotEmpty(), audio.isNotEmpty()) },
        combine(
            sentenceRepository.observeSentences(topicId),
            storyRepository.observeStories(topicId),
            imageContentRepository.observeImages(topicId),
        ) { sentences, stories, images -> Triple(sentences.isNotEmpty(), stories.isNotEmpty(), images.isNotEmpty()) },
    ) { first, second ->
        TopicActionAvailability(
            hasWords = first.first,
            hasVideos = first.second,
            hasAudio = first.third,
            hasSentences = second.first,
            hasStories = second.second,
            hasImages = second.third,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TopicActionAvailability())
}
