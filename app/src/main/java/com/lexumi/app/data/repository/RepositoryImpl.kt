package com.lexumi.app.data.repository

import com.lexumi.app.data.local.dao.*
import com.lexumi.app.data.local.entity.*
import com.lexumi.app.domain.model.*
import com.lexumi.app.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// ---------- mappers ----------
private fun UserProfileEntity.toDomain() = UserProfile(id, displayName)
private fun LanguageEntity.toDomain() = Language(id, profileId, name, voiceName)
private fun SectionEntity.toDomain() = Section(id, languageId, name, position)
private fun TopicEntity.toDomain() = Topic(id, sectionId, name, position)
private fun RuleEntity.toDomain() = Rule(id, languageId, name, text, imagePath)
private fun WordEntity.toDomain() = Word(id, topicId, imagePath, term, translation, ruleId, rating, correctStreak, typedStreak, typedReverseActive, voiceStreak, finalStreak, timesSeen, inReviewList, totalCorrect, bestStreak, currentStatsStreak)
private fun ImageContentEntity.toDomain() = ImageContent(id, topicId, name, imagePath, translation)
private fun VideoEntity.toDomain() = VideoContent(id, topicId, name, youtubeUrl, localVideoPath, originalText, translationText, ruleIds)
private fun AudioDialogEntity.toDomain() = AudioDialog(id, topicId, name, audioPath, translationText, ruleIds)
private fun SentenceEntity.toDomain() = Sentence(id, topicId, name, text, translations, ruleIds, rating, directStreak, reverseStreak, audioStreak, voiceStreak, timesSeen, totalCorrect, bestStreak, currentStatsStreak, known)
private fun StoryEntity.toDomain() = Story(id, topicId, name, text, translation, ruleIds)
private fun TestQuestionEntity.toDomain() = TestQuestion(
    id, questionText,
    if (answerType == AnswerType.TRUE_FALSE) QuestionAnswerType.TRUE_FALSE else QuestionAnswerType.EXACT_TEXT,
    correctBoolean, acceptableAnswers,
)

class ProfileRepositoryImpl @Inject constructor(private val dao: UserProfileDao) : ProfileRepository {
    override fun observeProfiles(): Flow<List<UserProfile>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun createProfile(name: String): Long = dao.insert(UserProfileEntity(displayName = name))
    override suspend fun deleteProfile(profile: UserProfile) = dao.delete(UserProfileEntity(profile.id, profile.displayName))
    override suspend fun profileCount(): Int = dao.count()
    override suspend fun profileExists(id: Long): Boolean = dao.getById(id) != null
}

class LanguageRepositoryImpl @Inject constructor(private val dao: LanguageDao) : LanguageRepository {
    override fun observeLanguages(profileId: Long): Flow<List<Language>> =
        dao.observeAll(profileId).map { list -> list.map { it.toDomain() } }
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

class WordRepositoryImpl @Inject constructor(private val dao: WordDao) : WordRepository {
    override fun observeWords(topicId: Long): Flow<List<Word>> =
        dao.observeForTopic(topicId).map { list -> list.map { it.toDomain() } }
    override suspend fun getWords(topicId: Long): List<Word> = dao.getForTopic(topicId).map { it.toDomain() }
    override suspend fun getWord(id: Long): Word? = dao.getById(id)?.toDomain()
    override suspend fun exists(topicId: Long, term: String): Boolean = dao.countByTerm(topicId, term) > 0
    override suspend fun addWord(topicId: Long, imagePath: String?, term: String, translation: String, ruleId: Long?): Long =
        dao.insert(WordEntity(topicId = topicId, imagePath = imagePath, term = term, translation = translation, ruleId = ruleId))
    override suspend fun updateWord(word: Word) {
        dao.update(
            WordEntity(
                id = word.id, topicId = word.topicId, imagePath = word.imagePath, term = word.term,
                translation = word.translation, ruleId = word.ruleId, rating = word.rating,
                correctStreak = word.correctStreak, typedStreak = word.typedStreak,
                typedReverseActive = word.typedReverseActive, voiceStreak = word.voiceStreak,
                finalStreak = word.finalStreak, timesSeen = word.timesSeen,
                lastSeenAt = System.currentTimeMillis(), inReviewList = word.inReviewList,
                addedToReviewAt = if (word.inReviewList) System.currentTimeMillis() else null,
                totalCorrect = word.totalCorrect, bestStreak = word.bestStreak, currentStatsStreak = word.currentStatsStreak,
            )
        )
    }
    override fun observeReviewList(): Flow<List<Word>> = dao.observeReviewList().map { list -> list.map { it.toDomain() } }
    override suspend fun deleteWord(word: Word) {
        dao.delete(
            WordEntity(
                id = word.id, topicId = word.topicId, imagePath = word.imagePath, term = word.term,
                translation = word.translation, ruleId = word.ruleId, rating = word.rating,
                correctStreak = word.correctStreak, typedStreak = word.typedStreak,
                typedReverseActive = word.typedReverseActive, voiceStreak = word.voiceStreak,
                finalStreak = word.finalStreak, timesSeen = word.timesSeen,
                inReviewList = word.inReviewList,
                totalCorrect = word.totalCorrect, bestStreak = word.bestStreak, currentStatsStreak = word.currentStatsStreak,
            )
        )
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
        topicId: Long, name: String, youtubeUrl: String?, localVideoPath: String?, originalText: String?,
        translationText: String?, ruleIds: List<Long>, questions: List<TestQuestion>,
    ): Long {
        val id = dao.insert(
            VideoEntity(topicId = topicId, name = name, youtubeUrl = youtubeUrl, localVideoPath = localVideoPath,
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

class SentenceRepositoryImpl @Inject constructor(private val dao: SentenceDao) : SentenceRepository {
    override fun observeSentences(topicId: Long): Flow<List<Sentence>> =
        dao.observeForTopic(topicId).map { list -> list.map { it.toDomain() } }
    override suspend fun getSentences(topicId: Long): List<Sentence> = dao.getForTopic(topicId).map { it.toDomain() }
    override suspend fun exists(topicId: Long, name: String): Boolean = dao.countByName(topicId, name) > 0
    override suspend fun addSentence(topicId: Long, name: String, text: String, translations: List<String>, ruleIds: List<Long>): Long =
        dao.insert(SentenceEntity(topicId = topicId, name = name, text = text, translations = translations, ruleIds = ruleIds))
    override suspend fun updateStats(sentence: Sentence) {
        dao.update(
            SentenceEntity(
                id = sentence.id, topicId = sentence.topicId, name = sentence.name, text = sentence.text,
                translations = sentence.translations, ruleIds = sentence.ruleIds, rating = sentence.rating,
                directStreak = sentence.directStreak, reverseStreak = sentence.reverseStreak,
                audioStreak = sentence.audioStreak, voiceStreak = sentence.voiceStreak,
                timesSeen = sentence.timesSeen, totalCorrect = sentence.totalCorrect,
                bestStreak = sentence.bestStreak, currentStatsStreak = sentence.currentStatsStreak,
                known = sentence.known,
            )
        )
    }
    override suspend fun deleteSentence(sentence: Sentence) {
        dao.delete(
            SentenceEntity(
                id = sentence.id, topicId = sentence.topicId, name = sentence.name, text = sentence.text,
                translations = sentence.translations, ruleIds = sentence.ruleIds,
                timesSeen = sentence.timesSeen, totalCorrect = sentence.totalCorrect,
                bestStreak = sentence.bestStreak, currentStatsStreak = sentence.currentStatsStreak,
                known = sentence.known,
            )
        )
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
