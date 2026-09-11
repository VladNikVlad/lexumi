package com.lexumi.app.domain.repository

import com.lexumi.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfiles(): Flow<List<UserProfile>>
    suspend fun createProfile(name: String): Long
    suspend fun renameProfile(id: Long, name: String)
    suspend fun deleteProfile(profile: UserProfile)
    suspend fun profileCount(): Int
    suspend fun profileExists(id: Long): Boolean
    /** "user1", "user2"... — the next unused default name, based on the highest "userN" already taken. */
    suspend fun nextDefaultProfileName(): String
}

interface LanguageRepository {
    /** All languages that exist locally on this device, regardless of which profile created them. */
    fun observeLanguages(): Flow<List<Language>>
    suspend fun getLanguage(id: Long): Language?
    suspend fun exists(profileId: Long, name: String): Boolean
    suspend fun addLanguage(profileId: Long, name: String): Long
    suspend fun setVoice(languageId: Long, voiceName: String?)
}

interface SectionRepository {
    fun observeSections(languageId: Long): Flow<List<Section>>
    /** "Самостійне вивчення" — sections synced from an admin-published language, always read-only. */
    fun observeAdminSections(languageId: Long): Flow<List<Section>>
    /** "Власний матеріал" — sections the user created themselves, never touched by sync. */
    fun observePersonalSections(languageId: Long): Flow<List<Section>>
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
    /** Persists a new drag-and-drop order — [orderedIds] is the full, final top-to-bottom order. */
    suspend fun reorderTopics(orderedIds: List<Long>)
}

interface RuleRepository {
    fun observeRules(languageId: Long): Flow<List<Rule>>
    suspend fun getRulesByIds(ids: List<Long>): List<Rule>
    suspend fun getRule(id: Long): Rule?
    suspend fun exists(languageId: Long, name: String): Boolean
    suspend fun addRule(languageId: Long, name: String, text: String, imagePath: String? = null): Long
}

interface WordRepository {
    fun observeWords(topicId: Long): Flow<List<Word>>
    suspend fun getWords(topicId: Long): List<Word>
    /** Every word that exists anywhere in this language — regardless of topic. Used for e.g. the
     * multiple-choice distractor pool, which no longer makes sense scoped to a single topic since
     * a word doesn't belong to just one. */
    suspend fun getWordsForLanguage(languageId: Long): List<Word>
    suspend fun getWord(topicId: Long, id: Long): Word?
    suspend fun findByLanguageAndTerm(languageId: Long, term: String): Word?
    /** True if this exact topic already links to a word with this term (not whether the term
     * exists elsewhere in the language — reusing a term across topics is the whole point). */
    suspend fun exists(topicId: Long, term: String): Boolean
    suspend fun addWord(topicId: Long, imagePath: String?, term: String, translation: String, ruleId: Long?): Long
    /** Progress-only update (rating/streaks/review-list/etc.) — never touches term/translations/
     * imagePath/ruleId, so it's safe to call with a `word` whose `translation` is topic-resolved. */
    suspend fun updateWord(word: Word)
    /** Saves an edited term/translation/image/rule from within [topicId]'s context. If this topic
     * already forked its own translation, only that override changes; otherwise the shared
     * default (`translations[0]`) changes, visible to every topic that hasn't forked. */
    suspend fun editWord(topicId: Long, wordId: Long, term: String, translation: String, imagePath: String?, ruleId: Long?)
    /** Forks this topic's translation away from the shared default, without touching it or any
     * other topic ("Додати локальний переклад"). */
    suspend fun forkTranslation(topicId: Long, wordId: Long, translation: String)
    suspend fun deleteWord(word: Word, topicId: Long)
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
        topicId: Long, name: String, youtubeUrl: String?, originalText: String?,
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
    suspend fun getSentence(topicId: Long, id: Long): Sentence?
    suspend fun findByLanguageAndText(languageId: Long, text: String): Sentence?
    /** True if this exact topic already links to a sentence with this text (reusing text across
     * topics is expected — see [WordRepository.exists] for the same reasoning). */
    suspend fun exists(topicId: Long, text: String): Boolean
    suspend fun addSentence(topicId: Long, text: String, translations: List<String>, ruleIds: List<Long>): Long
    /** Progress-only update — never touches text/translations/ruleIds (see [WordRepository.updateWord]). */
    suspend fun updateStats(sentence: Sentence)
    /** Saves an edited text/translations/rules from within [topicId]'s context — same
     * shared-vs-override rule as [WordRepository.editWord]. */
    suspend fun editSentence(topicId: Long, sentenceId: Long, text: String, translations: List<String>, ruleIds: List<Long>)
    suspend fun forkTranslations(topicId: Long, sentenceId: Long, translations: List<String>)
    suspend fun deleteSentence(sentence: Sentence, topicId: Long)
}

interface StoryRepository {
    fun observeStories(topicId: Long): Flow<List<Story>>
    suspend fun getStory(id: Long): Story?
    suspend fun exists(topicId: Long, name: String): Boolean
    suspend fun addStory(topicId: Long, name: String, text: String, translation: String?, ruleIds: List<Long>): Long
}
