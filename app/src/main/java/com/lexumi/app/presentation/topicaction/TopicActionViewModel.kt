package com.lexumi.app.presentation.topicaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.data.network.ConnectivityChecker
import com.lexumi.app.data.sync.ContentSyncRepository
import com.lexumi.app.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    private val syncRepository: ContentSyncRepository,
    private val connectivityChecker: ConnectivityChecker,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val topicId: Long = checkNotNull(savedStateHandle["topicId"])

    private val _topicName = MutableStateFlow("")
    val topicName: StateFlow<String> = _topicName

    // Fail-closed: hidden until the topic's own remoteId is known — synced from the admin
    // (remoteId != null) means always read-only, created by the user means always editable.
    // No admin check anymore: editing admin content is a web-panel-only job now.
    private val _canEdit = MutableStateFlow(false)
    val canEdit: StateFlow<Boolean> = _canEdit

    // True once we've determined this is an admin topic with no local content AND no network to
    // fetch it — TopicActionScreen shows "недоступно офлайн" instead of the action menu then.
    // Stays false for "Власний матеріал" topics (always fully local, never gated).
    private val _offlineUnavailable = MutableStateFlow(false)
    val offlineUnavailable: StateFlow<Boolean> = _offlineUnavailable

    init {
        viewModelScope.launch {
            val topic = topicRepository.getTopic(topicId)
            _topicName.value = topic?.name.orEmpty()
            val isAdminTopic = topic?.remoteId != null
            _canEdit.value = !isAdminTopic

            // Opening this topic is what makes it "the one topic kept offline-ready" — evicts
            // every other admin topic's cached content and (re)syncs this one, per the "Duolingo
            // model" plan notes. Offline with nothing cached yet for this topic is the one case
            // where there's genuinely nothing to show.
            if (isAdminTopic) {
                if (connectivityChecker.isOnline()) {
                    runCatching {
                        syncRepository.evictOtherTopicsContent(topicId)
                        syncRepository.syncTopicContent(topicId)
                    }
                    _offlineUnavailable.value = false
                } else {
                    val hasLocalContent = wordRepository.getWords(topicId).isNotEmpty() ||
                        sentenceRepository.getSentences(topicId).isNotEmpty() ||
                        videoRepository.observeVideos(topicId).first().isNotEmpty() ||
                        storyRepository.observeStories(topicId).first().isNotEmpty() ||
                        imageContentRepository.observeImages(topicId).first().isNotEmpty() ||
                        audioDialogRepository.observeDialogs(topicId).first().isNotEmpty()
                    _offlineUnavailable.value = !hasLocalContent
                }
            }
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
