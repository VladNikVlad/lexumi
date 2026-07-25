package com.lexumi.app.domain.repository

import com.lexumi.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfiles(): Flow<List<UserProfile>>
    suspend fun createProfile(name: String): Long
    suspend fun deleteProfile(profile: UserProfile)
    suspend fun profileCount(): Int
}

interface LanguageRepository {
    fun observeLanguages(profileId: Long): Flow<List<Language>>
    suspend fun getLanguage(id: Long): Language?
    suspend fun exists(profileId: Long, name: String): Boolean
    suspend fun addLanguage(profileId: Long, name: String): Long
}

interface SectionRepository {
    fun observeSections(languageId: Long): Flow<List<Section>>
    suspend fun getSection(id: Long): Section?
    suspend fun exists(languageId: Long, name: String): Boolean
    suspend fun addSection(languageId: Long, name: String): Long
    suspend fun sectionCount(languageId: Long): Int
}

interface TopicRepository {
    fun observeTopics(sectionId: Long): Flow<List<Topic>>
    suspend fun getTopic(id: Long): Topic?
    suspend fun exists(sectionId: Long, name: String): Boolean
    suspend fun addTopic(sectionId: Long, name: String): Long
}

interface RuleRepository {
    fun observeRules(languageId: Long): Flow<List<Rule>>
    suspend fun getRulesByIds(ids: List<Long>): List<Rule>
    suspend fun getRule(id: Long): Rule?
    suspend fun exists(languageId: Long, name: String): Boolean
    suspend fun addRule(languageId: Long, name: String, text: String): Long
}

interface WordRepository {
    fun observeWords(topicId: Long): Flow<List<Word>>
    suspend fun getWords(topicId: Long): List<Word>
    suspend fun exists(topicId: Long, term: String): Boolean
    suspend fun addWord(topicId: Long, imagePath: String?, term: String, translation: String, ruleId: Long?): Long
    suspend fun updateWord(word: Word)
    fun observeReviewList(): Flow<List<Word>>
}

interface ImageContentRepository {
    fun observeImages(topicId: Long): Flow<List<ImageContent>>
    suspend fun exists(topicId: Long, name: String): Boolean
    suspend fun addImage(topicId: Long, name: String, imagePath: String, translation: String): Long
}

interface VideoRepository {
    fun observeVideos(topicId: Long): Flow<List<VideoContent>>
    suspend fun getVideo(id: Long): VideoContent?
    suspend fun exists(topicId: Long, name: String): Boolean
    suspend fun addVideo(
        topicId: Long, name: String, youtubeUrl: String, originalText: String?,
        translationText: String?, ruleIds: List<Long>, questions: List<TestQuestion>,
    ): Long
    suspend fun getQuestions(videoId: Long): List<TestQuestion>
}

interface AudioDialogRepository {
    fun observeDialogs(topicId: Long): Flow<List<AudioDialog>>
    suspend fun getDialog(id: Long): AudioDialog?
    suspend fun exists(topicId: Long, name: String): Boolean
    suspend fun addDialog(
        topicId: Long, name: String, audioPath: String, translationText: String?,
        ruleIds: List<Long>, questions: List<TestQuestion>,
    ): Long
    suspend fun getQuestions(dialogId: Long): List<TestQuestion>
}

interface SentenceRepository {
    fun observeSentences(topicId: Long): Flow<List<Sentence>>
    suspend fun getSentences(topicId: Long): List<Sentence>
    suspend fun exists(topicId: Long, name: String): Boolean
    suspend fun addSentence(topicId: Long, name: String, text: String, translations: List<String>, ruleIds: List<Long>): Long
    suspend fun updateScore(sentenceId: Long, newScore: Double, timesSeen: Int)
}

interface StoryRepository {
    fun observeStories(topicId: Long): Flow<List<Story>>
    suspend fun getStory(id: Long): Story?
    suspend fun exists(topicId: Long, name: String): Boolean
    suspend fun addStory(topicId: Long, name: String, text: String, translation: String?, ruleIds: List<Long>): Long
}
