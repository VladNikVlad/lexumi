package com.lexumi.app.data.repository

import com.lexumi.app.data.local.dao.*
import com.lexumi.app.data.local.entity.*
import com.lexumi.app.domain.model.*
import com.lexumi.app.domain.repository.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// ---------- mappers ----------
private fun UserProfileEntity.toDomain() = UserProfile(id, displayName)
private fun LanguageEntity.toDomain() = Language(id, profileId, name, voiceName, remoteId)
private fun SectionEntity.toDomain() = Section(id, languageId, name, position, remoteId)
private fun TopicEntity.toDomain() = Topic(id, sectionId, name, position, remoteId)
private fun RuleEntity.toDomain() = Rule(id, languageId, name, text, imagePath, remoteId)
/** No topic context — resolves to the shared default translation (no override). */
private fun WordEntity.toDomain() = Word(id, languageId, imagePath, translations.firstOrNull().orEmpty(), translations, term, ruleId, rating, correctStreak, typedStreak, typedReverseActive, voiceStreak, finalStreak, timesSeen, inReviewList, totalCorrect, bestStreak, currentStatsStreak, remoteId)
private fun WordEntity.toDomain(crossRef: WordTopicCrossRefEntity) = Word(
    id, languageId, imagePath, crossRef.translationOverride ?: translations.firstOrNull().orEmpty(), translations, term, ruleId,
    rating, correctStreak, typedStreak, typedReverseActive, voiceStreak, finalStreak, timesSeen, inReviewList, totalCorrect,
    bestStreak, currentStatsStreak, remoteId,
)
private fun ImageContentEntity.toDomain() = ImageContent(id, topicId, name, imagePath, translation, remoteId)
private fun VideoEntity.toDomain() = VideoContent(id, topicId, name, youtubeUrl, localVideoPath, originalText, translationText, ruleIds, remoteId)
private fun AudioDialogEntity.toDomain() = AudioDialog(id, topicId, name, audioPath, translationText, ruleIds, remoteId)
/** No topic context — resolves to the sentence's own shared translations (no override). */
private fun SentenceEntity.toDomain() = Sentence(id, languageId, text, translations, ruleIds, rating, directStreak, reverseStreak, audioStreak, voiceStreak, timesSeen, totalCorrect, bestStreak, currentStatsStreak, known, remoteId)
private fun SentenceEntity.toDomain(crossRef: SentenceTopicCrossRefEntity) = Sentence(
    id, languageId, text, crossRef.translationsOverride ?: translations, ruleIds, rating, directStreak, reverseStreak,
    audioStreak, voiceStreak, timesSeen, totalCorrect, bestStreak, currentStatsStreak, known, remoteId,
)
private fun StoryEntity.toDomain() = Story(id, topicId, name, text, translation, ruleIds, remoteId)
private fun TestQuestionEntity.toDomain() = TestQuestion(
    id, questionText,
    if (answerType == AnswerType.TRUE_FALSE) QuestionAnswerType.TRUE_FALSE else QuestionAnswerType.EXACT_TEXT,
    correctBoolean, acceptableAnswers, remoteId,
)

class ProfileRepositoryImpl @Inject constructor(private val dao: UserProfileDao) : ProfileRepository {
    override fun observeProfiles(): Flow<List<UserProfile>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun createProfile(name: String): Long = dao.insert(UserProfileEntity(displayName = name))
    override suspend fun renameProfile(id: Long, name: String) = dao.rename(id, name)
    override suspend fun deleteProfile(profile: UserProfile) = dao.delete(UserProfileEntity(profile.id, profile.displayName))
    override suspend fun profileCount(): Int = dao.count()
    override suspend fun profileExists(id: Long): Boolean = dao.getById(id) != null
    override suspend fun nextDefaultProfileName(): String {
        val highest = dao.getAllDisplayNames()
            .mapNotNull { Regex("^user(\\d+)$").matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }
            .maxOrNull() ?: 0
        return "user${highest + 1}"
    }
}

class LanguageRepositoryImpl @Inject constructor(private val dao: LanguageDao) : LanguageRepository {
    override fun observeLanguages(): Flow<List<Language>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun getLanguage(id: Long): Language? = dao.getById(id)?.toDomain()
    override suspend fun exists(profileId: Long, name: String): Boolean = dao.countByName(profileId, name) > 0
    override suspend fun addLanguage(profileId: Long, name: String): Long =
        dao.insert(LanguageEntity(profileId = profileId, name = name))
    override suspend fun setVoice(languageId: Long, voiceName: String?) = dao.setVoice(languageId, voiceName)
}

class SectionRepositoryImpl @Inject constructor(private val dao: SectionDao) : SectionRepository {
    override fun observeSections(languageId: Long): Flow<List<Section>> =
        dao.observeForLanguage(languageId).map { list -> list.map { it.toDomain() } }
    override suspend fun getSection(id: Long): Section? = dao.getById(id)?.toDomain()
    override suspend fun exists(languageId: Long, name: String): Boolean = dao.countByName(languageId, name) > 0
    override suspend fun addSection(languageId: Long, name: String): Long {
        val position = dao.countForLanguage(languageId)
        return dao.insert(SectionEntity(languageId = languageId, name = name, position = position))
    }
    override suspend fun sectionCount(languageId: Long): Int = dao.countForLanguage(languageId)
}

class TopicRepositoryImpl @Inject constructor(private val dao: TopicDao) : TopicRepository {
    override fun observeTopics(sectionId: Long): Flow<List<Topic>> =
        dao.observeForSection(sectionId).map { list -> list.map { it.toDomain() } }
    override suspend fun getTopic(id: Long): Topic? = dao.getById(id)?.toDomain()
    override suspend fun exists(sectionId: Long, name: String): Boolean = dao.countByName(sectionId, name) > 0
    override suspend fun addTopic(sectionId: Long, name: String): Long =
        dao.insert(TopicEntity(sectionId = sectionId, name = name))
    override suspend fun reorderTopics(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> dao.updatePosition(id, index) }
    }
}

class RuleRepositoryImpl @Inject constructor(private val dao: RuleDao) : RuleRepository {
    override fun observeRules(languageId: Long): Flow<List<Rule>> =
        dao.observeForLanguage(languageId).map { list -> list.map { it.toDomain() } }
    override suspend fun getRulesByIds(ids: List<Long>): List<Rule> =
        if (ids.isEmpty()) emptyList() else dao.getByIds(ids).map { it.toDomain() }
    override suspend fun getRule(id: Long): Rule? = dao.getById(id)?.toDomain()
    override suspend fun exists(languageId: Long, name: String): Boolean = dao.countByName(languageId, name) > 0
    override suspend fun addRule(languageId: Long, name: String, text: String, imagePath: String?): Long =
        dao.insert(RuleEntity(languageId = languageId, name = name, text = text, imagePath = imagePath))
}

@OptIn(ExperimentalCoroutinesApi::class)
class WordRepositoryImpl @Inject constructor(
    private val dao: WordDao,
    private val crossRefDao: WordTopicCrossRefDao,
    private val topicDao: TopicDao,
    private val sectionDao: SectionDao,
) : WordRepository {

    private suspend fun languageIdForTopic(topicId: Long): Long {
        val topic = checkNotNull(topicDao.getById(topicId)) { "Topic $topicId not found" }
        return checkNotNull(sectionDao.getById(topic.sectionId)) { "Section ${topic.sectionId} not found" }.languageId
    }

    private fun resolve(crossRefs: List<WordTopicCrossRefEntity>, words: List<WordEntity>): List<Word> {
        val byId = words.associateBy { it.id }
        return crossRefs.sortedBy { it.position }.mapNotNull { cr -> byId[cr.wordId]?.toDomain(cr) }
    }

    override fun observeWords(topicId: Long): Flow<List<Word>> =
        crossRefDao.observeForTopic(topicId).flatMapLatest { crossRefs ->
            if (crossRefs.isEmpty()) flowOf(emptyList())
            else dao.observeByIds(crossRefs.map { it.wordId }).map { words -> resolve(crossRefs, words) }
        }

    override suspend fun getWords(topicId: Long): List<Word> {
        val crossRefs = crossRefDao.getForTopic(topicId)
        if (crossRefs.isEmpty()) return emptyList()
        return resolve(crossRefs, dao.getByIds(crossRefs.map { it.wordId }))
    }

    override suspend fun getWordsForLanguage(languageId: Long): List<Word> = dao.getForLanguage(languageId).map { it.toDomain() }

    override suspend fun getWord(topicId: Long, id: Long): Word? {
        val entity = dao.getById(id) ?: return null
        val crossRef = crossRefDao.getLink(topicId, id)
        return if (crossRef != null) entity.toDomain(crossRef) else entity.toDomain()
    }

    override suspend fun findByLanguageAndTerm(languageId: Long, term: String): Word? = dao.findByLanguageAndTerm(languageId, term)?.toDomain()

    override suspend fun exists(topicId: Long, term: String): Boolean {
        val languageId = languageIdForTopic(topicId)
        val word = dao.findByLanguageAndTerm(languageId, term) ?: return false
        return crossRefDao.getLink(topicId, word.id) != null
    }

    override suspend fun addWord(topicId: Long, imagePath: String?, term: String, translation: String, ruleId: Long?): Long {
        val languageId = languageIdForTopic(topicId)
        val existing = dao.findByLanguageAndTerm(languageId, term)
        val wordId = existing?.id ?: dao.insert(
            WordEntity(languageId = languageId, imagePath = imagePath, term = term.trim(), translations = listOf(translation), ruleId = ruleId),
        )
        val override = if (existing != null && !existing.translations.firstOrNull().orEmpty().equals(translation, ignoreCase = true)) translation else null
        val position = crossRefDao.countForTopic(topicId)
        crossRefDao.insert(WordTopicCrossRefEntity(topicId = topicId, wordId = wordId, translationOverride = override, position = position))
        return wordId
    }

    /** Progress-only — reads the current row and overwrites only rating/streak/review fields, so a
     * topic-resolved `word.translation` can never leak into the shared `translations` list. */
    override suspend fun updateWord(word: Word) {
        val current = dao.getById(word.id) ?: return
        dao.update(
            current.copy(
                rating = word.rating, correctStreak = word.correctStreak, typedStreak = word.typedStreak,
                typedReverseActive = word.typedReverseActive, voiceStreak = word.voiceStreak, finalStreak = word.finalStreak,
                timesSeen = word.timesSeen, lastSeenAt = System.currentTimeMillis(), inReviewList = word.inReviewList,
                addedToReviewAt = if (word.inReviewList) System.currentTimeMillis() else null,
                totalCorrect = word.totalCorrect, bestStreak = word.bestStreak, currentStatsStreak = word.currentStatsStreak,
            )
        )
    }

    override suspend fun editWord(topicId: Long, wordId: Long, term: String, translation: String, imagePath: String?, ruleId: Long?) {
        val current = dao.getById(wordId) ?: return
        val link = crossRefDao.getLink(topicId, wordId)
        if (link?.translationOverride != null) {
            crossRefDao.update(link.copy(translationOverride = translation))
            dao.update(current.copy(term = term, imagePath = imagePath, ruleId = ruleId))
        } else {
            val newTranslations = listOf(translation) + current.translations.drop(1)
            dao.update(current.copy(term = term, imagePath = imagePath, ruleId = ruleId, translations = newTranslations))
        }
    }

    override suspend fun forkTranslation(topicId: Long, wordId: Long, translation: String) {
        val link = crossRefDao.getLink(topicId, wordId) ?: return
        crossRefDao.update(link.copy(translationOverride = translation))
    }

    override fun observeReviewList(): Flow<List<Word>> = dao.observeReviewList().map { list -> list.map { it.toDomain() } }

    override suspend fun deleteWord(word: Word, topicId: Long) {
        crossRefDao.deleteLink(topicId, word.id)
        if (dao.countLinks(word.id) == 0) dao.deleteById(word.id)
    }
}

class ImageContentRepositoryImpl @Inject constructor(private val dao: ImageContentDao) : ImageContentRepository {
    override fun observeImages(topicId: Long): Flow<List<ImageContent>> =
        dao.observeForTopic(topicId).map { list -> list.map { it.toDomain() } }
    override suspend fun exists(topicId: Long, name: String): Boolean = dao.countByName(topicId, name) > 0
    override suspend fun addImage(topicId: Long, name: String, imagePath: String, translation: String): Long =
        dao.insert(ImageContentEntity(topicId = topicId, name = name, imagePath = imagePath, translation = translation))
}

class VideoRepositoryImpl @Inject constructor(
    private val dao: VideoDao,
    private val questionDao: TestQuestionDao,
) : VideoRepository {
    override fun observeVideos(topicId: Long): Flow<List<VideoContent>> =
        dao.observeForTopic(topicId).map { list -> list.map { it.toDomain() } }
    override suspend fun getVideo(id: Long): VideoContent? = dao.getById(id)?.toDomain()
    override suspend fun exists(topicId: Long, name: String): Boolean = dao.countByName(topicId, name) > 0
    override suspend fun addVideo(
        topicId: Long, name: String, youtubeUrl: String?, originalText: String?,
        translationText: String?, ruleIds: List<Long>, questions: List<TestQuestion>,
    ): Long {
        val id = dao.insert(
            VideoEntity(topicId = topicId, name = name, youtubeUrl = youtubeUrl, localVideoPath = null,
                originalText = originalText, translationText = translationText, ruleIds = ruleIds)
        )
        if (questions.isNotEmpty()) {
            questionDao.insertAll(questions.map {
                TestQuestionEntity(
                    ownerType = QuestionOwnerType.VIDEO, ownerId = id, questionText = it.questionText,
                    answerType = if (it.answerType == QuestionAnswerType.TRUE_FALSE) AnswerType.TRUE_FALSE else AnswerType.EXACT_TEXT,
                    correctBoolean = it.correctBoolean, acceptableAnswers = it.acceptableAnswers,
                )
            })
        }
        return id
    }
    override suspend fun getQuestions(videoId: Long): List<TestQuestion> =
        questionDao.getForOwner(QuestionOwnerType.VIDEO, videoId).map { it.toDomain() }
}

class AudioDialogRepositoryImpl @Inject constructor(
    private val dao: AudioDialogDao,
    private val questionDao: TestQuestionDao,
) : AudioDialogRepository {
    override fun observeDialogs(topicId: Long): Flow<List<AudioDialog>> =
        dao.observeForTopic(topicId).map { list -> list.map { it.toDomain() } }
    override suspend fun getDialog(id: Long): AudioDialog? = dao.getById(id)?.toDomain()
    override suspend fun exists(topicId: Long, name: String): Boolean = dao.countByName(topicId, name) > 0
    override suspend fun addDialog(
        topicId: Long, name: String, audioPath: String, translationText: String?,
        ruleIds: List<Long>, questions: List<TestQuestion>,
    ): Long {
        val id = dao.insert(AudioDialogEntity(topicId = topicId, name = name, audioPath = audioPath,
            translationText = translationText, ruleIds = ruleIds))
        if (questions.isNotEmpty()) {
            questionDao.insertAll(questions.map {
                TestQuestionEntity(
                    ownerType = QuestionOwnerType.AUDIO_DIALOG, ownerId = id, questionText = it.questionText,
                    answerType = if (it.answerType == QuestionAnswerType.TRUE_FALSE) AnswerType.TRUE_FALSE else AnswerType.EXACT_TEXT,
                    correctBoolean = it.correctBoolean, acceptableAnswers = it.acceptableAnswers,
                )
            })
        }
        return id
    }
    override suspend fun getQuestions(dialogId: Long): List<TestQuestion> =
        questionDao.getForOwner(QuestionOwnerType.AUDIO_DIALOG, dialogId).map { it.toDomain() }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SentenceRepositoryImpl @Inject constructor(
    private val dao: SentenceDao,
    private val crossRefDao: SentenceTopicCrossRefDao,
    private val topicDao: TopicDao,
    private val sectionDao: SectionDao,
) : SentenceRepository {

    private suspend fun languageIdForTopic(topicId: Long): Long {
        val topic = checkNotNull(topicDao.getById(topicId)) { "Topic $topicId not found" }
        return checkNotNull(sectionDao.getById(topic.sectionId)) { "Section ${topic.sectionId} not found" }.languageId
    }

    private fun resolve(crossRefs: List<SentenceTopicCrossRefEntity>, sentences: List<SentenceEntity>): List<Sentence> {
        val byId = sentences.associateBy { it.id }
        return crossRefs.sortedBy { it.position }.mapNotNull { cr -> byId[cr.sentenceId]?.toDomain(cr) }
    }

    override fun observeSentences(topicId: Long): Flow<List<Sentence>> =
        crossRefDao.observeForTopic(topicId).flatMapLatest { crossRefs ->
            if (crossRefs.isEmpty()) flowOf(emptyList())
            else dao.observeByIds(crossRefs.map { it.sentenceId }).map { sentences -> resolve(crossRefs, sentences) }
        }

    override suspend fun getSentences(topicId: Long): List<Sentence> {
        val crossRefs = crossRefDao.getForTopic(topicId)
        if (crossRefs.isEmpty()) return emptyList()
        return resolve(crossRefs, dao.getByIds(crossRefs.map { it.sentenceId }))
    }

    override suspend fun getSentence(topicId: Long, id: Long): Sentence? {
        val entity = dao.getById(id) ?: return null
        val crossRef = crossRefDao.getLink(topicId, id)
        return if (crossRef != null) entity.toDomain(crossRef) else entity.toDomain()
    }

    override suspend fun findByLanguageAndText(languageId: Long, text: String): Sentence? = dao.findByLanguageAndText(languageId, text)?.toDomain()

    override suspend fun exists(topicId: Long, text: String): Boolean {
        val languageId = languageIdForTopic(topicId)
        val sentence = dao.findByLanguageAndText(languageId, text) ?: return false
        return crossRefDao.getLink(topicId, sentence.id) != null
    }

    override suspend fun addSentence(topicId: Long, text: String, translations: List<String>, ruleIds: List<Long>): Long {
        val languageId = languageIdForTopic(topicId)
        val existing = dao.findByLanguageAndText(languageId, text)
        val sentenceId = existing?.id ?: dao.insert(
            SentenceEntity(languageId = languageId, text = text, translations = translations, ruleIds = ruleIds),
        )
        val override = if (existing != null && existing.translations != translations) translations else null
        val position = crossRefDao.countForTopic(topicId)
        crossRefDao.insert(SentenceTopicCrossRefEntity(topicId = topicId, sentenceId = sentenceId, translationsOverride = override, position = position))
        return sentenceId
    }

    /** Progress-only — see [WordRepositoryImpl.updateWord]. */
    override suspend fun updateStats(sentence: Sentence) {
        val current = dao.getById(sentence.id) ?: return
        dao.update(
            current.copy(
                rating = sentence.rating, directStreak = sentence.directStreak, reverseStreak = sentence.reverseStreak,
                audioStreak = sentence.audioStreak, voiceStreak = sentence.voiceStreak, timesSeen = sentence.timesSeen,
                totalCorrect = sentence.totalCorrect, bestStreak = sentence.bestStreak,
                currentStatsStreak = sentence.currentStatsStreak, known = sentence.known,
            )
        )
    }

    override suspend fun editSentence(topicId: Long, sentenceId: Long, text: String, translations: List<String>, ruleIds: List<Long>) {
        val current = dao.getById(sentenceId) ?: return
        val link = crossRefDao.getLink(topicId, sentenceId)
        if (link?.translationsOverride != null) {
            crossRefDao.update(link.copy(translationsOverride = translations))
            dao.update(current.copy(text = text, ruleIds = ruleIds))
        } else {
            dao.update(current.copy(text = text, ruleIds = ruleIds, translations = translations))
        }
    }

    override suspend fun forkTranslations(topicId: Long, sentenceId: Long, translations: List<String>) {
        val link = crossRefDao.getLink(topicId, sentenceId) ?: return
        crossRefDao.update(link.copy(translationsOverride = translations))
    }

    override suspend fun deleteSentence(sentence: Sentence, topicId: Long) {
        crossRefDao.deleteLink(topicId, sentence.id)
        if (dao.countLinks(sentence.id) == 0) dao.deleteById(sentence.id)
    }
}

class StoryRepositoryImpl @Inject constructor(private val dao: StoryDao) : StoryRepository {
    override fun observeStories(topicId: Long): Flow<List<Story>> =
        dao.observeForTopic(topicId).map { list -> list.map { it.toDomain() } }
    override suspend fun getStory(id: Long): Story? = dao.getById(id)?.toDomain()
    override suspend fun exists(topicId: Long, name: String): Boolean = dao.countByName(topicId, name) > 0
    override suspend fun addStory(topicId: Long, name: String, text: String, translation: String?, ruleIds: List<Long>): Long =
        dao.insert(StoryEntity(topicId = topicId, name = name, text = text, translation = translation, ruleIds = ruleIds))
}
