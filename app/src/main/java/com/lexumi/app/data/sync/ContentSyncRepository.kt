package com.lexumi.app.data.sync

import android.content.Context
import android.util.Base64
import com.lexumi.app.data.auth.AuthRepository
import com.lexumi.app.data.local.dao.ImageContentDao
import com.lexumi.app.data.local.dao.LanguageDao
import com.lexumi.app.data.local.dao.RuleDao
import com.lexumi.app.data.local.dao.SectionDao
import com.lexumi.app.data.local.dao.SentenceDao
import com.lexumi.app.data.local.dao.StoryDao
import com.lexumi.app.data.local.dao.TestQuestionDao
import com.lexumi.app.data.local.dao.TopicDao
import com.lexumi.app.data.local.dao.VideoDao
import com.lexumi.app.data.local.dao.WordDao
import com.lexumi.app.data.local.entity.AnswerType
import com.lexumi.app.data.local.entity.ImageContentEntity
import com.lexumi.app.data.local.entity.LanguageEntity
import com.lexumi.app.data.local.entity.QuestionOwnerType
import com.lexumi.app.data.local.entity.RuleEntity
import com.lexumi.app.data.local.entity.SectionEntity
import com.lexumi.app.data.local.entity.SentenceEntity
import com.lexumi.app.data.local.entity.StoryEntity
import com.lexumi.app.data.local.entity.TestQuestionEntity
import com.lexumi.app.data.local.entity.TopicEntity
import com.lexumi.app.data.local.entity.VideoEntity
import com.lexumi.app.data.local.entity.WordEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// --- rows exactly as they exist on the Supabase side (see backend/SCHEMA.md) ---
// Only the columns this pass actually needs are listed — Postgres fills in every
// other column (rating, streaks, timestamps...) from its own defaults on insert.
// `translations`/`acceptable_answers` are stored as a single text column, joined with the same
// unit-separator () as the local Room `Converters.fromStringList`; `rule_ids` is a plain
// comma-joined list of remote rule uuids (uuids never contain a comma).

private const val LIST_SEPARATOR = "\u001F"

@Serializable
private data class RemoteLanguageRow(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    val name: String,
    @SerialName("voice_name") val voiceName: String? = null,
)

@Serializable
private data class RemoteSectionRow(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("language_id") val languageId: String,
    val name: String,
    val position: Int,
)

@Serializable
private data class RemoteTopicRow(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("section_id") val sectionId: String,
    val name: String,
    val position: Int,
)

@Serializable
private data class RemoteWordRow(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("topic_id") val topicId: String,
    val term: String,
    val translation: String,
    @SerialName("rule_id") val ruleId: String? = null,
    @SerialName("image_data") val imageData: String? = null,
)

@Serializable
private data class RemoteRuleRow(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("language_id") val languageId: String,
    val name: String,
    val text: String,
    @SerialName("image_data") val imageData: String? = null,
)

/** Image cards are capped at 100kb (see `util/ImageCompressor.kt`) — small enough to embed
 * directly as base64 rather than needing real file storage. */
@Serializable
private data class RemoteImageContentRow(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("topic_id") val topicId: String,
    val name: String,
    val translation: String,
    @SerialName("image_data") val imageData: String,
)

@Serializable
private data class RemoteSentenceRow(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("topic_id") val topicId: String,
    val text: String,
    val translations: String,
    @SerialName("rule_ids") val ruleIds: String? = null,
)

/** Videos are YouTube-link-only going forward — a locally-hosted file can't be published without
 * file storage, so `youtubeUrl` is required here (unlike the local `VideoEntity`, where it's still
 * nullable for old local-file rows that never get published). */
@Serializable
private data class RemoteVideoRow(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("topic_id") val topicId: String,
    val name: String,
    @SerialName("youtube_url") val youtubeUrl: String,
    @SerialName("original_text") val originalText: String? = null,
    @SerialName("translation_text") val translationText: String? = null,
    @SerialName("rule_ids") val ruleIds: String? = null,
)

@Serializable
private data class RemoteStoryRow(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("topic_id") val topicId: String,
    val name: String,
    val text: String,
    val translation: String? = null,
    @SerialName("rule_ids") val ruleIds: String? = null,
)

/** Only `video_id` for now — `test_questions` attached to an audio dialogue aren't published yet
 * since audio dialogues themselves aren't (need file storage, same as image content). */
@Serializable
private data class RemoteTestQuestionRow(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("video_id") val videoId: String,
    @SerialName("question_text") val questionText: String,
    @SerialName("answer_type") val answerType: String,
    @SerialName("correct_boolean") val correctBoolean: Boolean? = null,
    @SerialName("acceptable_answers") val acceptableAnswers: String? = null,
)

/** Decodes the `select(Columns.list("id"))` response after an insert — only `id` comes back, so
 * decoding into the full row type (whose other fields have no defaults) would fail. */
@Serializable
private data class InsertedId(val id: String)

/** A language available to download — shown to non-admin users so they can pick admin-authored content. */
data class DownloadableLanguage(val remoteId: String, val name: String, val voiceName: String?)

/** Result of a publish pass — names of videos that couldn't be published because they were
 * added as a local file rather than a YouTube link (see [ContentSyncRepository.publishLanguage]). */
data class PublishResult(val skippedVideoNames: List<String>)

private fun List<Long>.toRemoteRuleIds(localToRemote: Map<Long, String>): String? =
    mapNotNull { localToRemote[it] }.takeIf { it.isNotEmpty() }?.joinToString(",")

private fun String?.toLocalRuleIds(remoteToLocal: Map<String, Long>): List<Long> =
    this?.split(",")?.mapNotNull { remoteToLocal[it] } ?: emptyList()

/** Reads a local image file (word/rule/image-card imagePath) and base64-encodes it for upload —
 * these are always <=100kb (`util/ImageCompressor.kt`), so embedding them directly as text is
 * simpler than standing up real file storage. Returns null if there's no image or the file is gone. */
private fun encodeImageFile(path: String?): String? {
    if (path == null) return null
    val file = File(path)
    if (!file.exists()) return null
    return Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
}

/** Reverse of [encodeImageFile] — decodes base64 image data downloaded from Supabase into a new
 * local file, returning its absolute path (or null if there was no image data to decode). */
private fun decodeImageToFile(context: Context, base64: String?, prefix: String): String? {
    if (base64 == null) return null
    val bytes = Base64.decode(base64, Base64.NO_WRAP)
    val file = File(context.filesDir, "${prefix}_${System.currentTimeMillis()}_${(0..999999).random()}.jpg")
    file.writeBytes(bytes)
    return file.absolutePath
}

/**
 * Pushes an admin's local content up to Supabase as global (`owner_id = null`) content, and lets
 * any user pull already-published content down into their own local Room copy. This is
 * intentionally a one-way "publish, then download a copy" flow rather than live two-way sync —
 * each downloaded word/topic/etc. becomes an ordinary local row from then on (its own rating,
 * its own streaks), exactly like anything the user creates themselves. See backend/SCHEMA.md for
 * why per-user progress can't simply live on the shared/global row.
 *
 * Covers languages/sections/topics/words/rules/sentences/videos(YouTube-only)/stories/image cards
 * and the test questions attached to a video. Word/rule/image-card images are small (<=100kb,
 * enforced by `util/ImageCompressor.kt`) so they're embedded as base64 text directly in their row
 * instead of needing real file storage. audio_dialogs are the one thing still not covered — an
 * audio file is not size-capped the same way, so syncing it needs actual file storage
 * (Supabase Storage), which isn't implemented.
 *
 * Publishing is a repeatable upsert, not "insert once and never touch again": a row that already
 * has a `remoteId` gets its remote copy *updated* with the current local fields, rather than
 * skipped outright. Without this, a field added to the sync payload after a row was first
 * published (e.g. images, added after words/rules already had rows on the server) would never
 * reach an already-published row, and neither would any later edit to already-published content.
 */
@Singleton
class ContentSyncRepository @Inject constructor(
    private val supabase: SupabaseClient,
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val languageDao: LanguageDao,
    private val sectionDao: SectionDao,
    private val topicDao: TopicDao,
    private val wordDao: WordDao,
    private val ruleDao: RuleDao,
    private val sentenceDao: SentenceDao,
    private val videoDao: VideoDao,
    private val storyDao: StoryDao,
    private val testQuestionDao: TestQuestionDao,
    private val imageContentDao: ImageContentDao,
) {

    /** Upserts one row: updates the existing remote row if [remoteId] is already known, otherwise
     * inserts a new one and returns its freshly-assigned id. Either way, the returned id is the
     * row's true remote id — safe to persist locally and to use as a parent reference for children. */
    private suspend inline fun <reified T : Any> upsertRemote(table: String, remoteId: String?, row: T): String =
        if (remoteId != null) {
            supabase.from(table).update(row) { filter { eq("id", remoteId) } }
            remoteId
        } else {
            supabase.from(table).insert(row) { select(Columns.list("id")) }.decodeSingle<InsertedId>().id
        }

    /** Pushes this language and everything under it up to Supabase. Safe to call again later —
     * already-published rows get their remote copy refreshed (not duplicated) rather than
     * skipped, so a later edit or a newly-added image reaches Supabase too. Throws if the
     * signed-in user isn't an admin (also enforced server-side by Row Level Security either way). */
    suspend fun publishLanguage(languageId: Long): PublishResult {
        val profile = authRepository.getMyProfile()
        check(profile?.isAdmin == true) { "Публікувати може лише адмін" }

        val language = languageDao.getById(languageId) ?: return PublishResult(emptyList())
        val remoteLanguageId = upsertRemote(
            "languages", language.remoteId,
            RemoteLanguageRow(ownerId = null, name = language.name, voiceName = language.voiceName),
        )
        languageDao.setRemoteId(language.id, remoteLanguageId)

        // Rules are language-scoped (shared across topics), so they're published once per
        // language, before the topic loop, and referenced from words/sentences/videos/stories
        // via this local-id -> remote-id map.
        val ruleIdMap = mutableMapOf<Long, String>()
        for (rule in ruleDao.getForLanguage(languageId)) {
            val remoteRuleId = upsertRemote(
                "rules", rule.remoteId,
                RemoteRuleRow(
                    ownerId = null, languageId = remoteLanguageId, name = rule.name, text = rule.text,
                    imageData = encodeImageFile(rule.imagePath),
                ),
            )
            ruleDao.setRemoteId(rule.id, remoteRuleId)
            ruleIdMap[rule.id] = remoteRuleId
        }

        val skippedVideoNames = mutableListOf<String>()

        for (section in sectionDao.getForLanguage(languageId)) {
            val remoteSectionId = upsertRemote(
                "sections", section.remoteId,
                RemoteSectionRow(ownerId = null, languageId = remoteLanguageId, name = section.name, position = section.position),
            )
            sectionDao.setRemoteId(section.id, remoteSectionId)

            for (topic in topicDao.getForSection(section.id)) {
                val remoteTopicId = upsertRemote(
                    "topics", topic.remoteId,
                    RemoteTopicRow(ownerId = null, sectionId = remoteSectionId, name = topic.name, position = topic.position),
                )
                topicDao.setRemoteId(topic.id, remoteTopicId)

                for (word in wordDao.getForTopic(topic.id)) {
                    val remoteWordId = upsertRemote(
                        "words", word.remoteId,
                        RemoteWordRow(
                            ownerId = null, topicId = remoteTopicId, term = word.term, translation = word.translation,
                            ruleId = word.ruleId?.let { ruleIdMap[it] }, imageData = encodeImageFile(word.imagePath),
                        ),
                    )
                    wordDao.setRemoteId(word.id, remoteWordId)
                }

                for (sentence in sentenceDao.getForTopic(topic.id)) {
                    val remoteSentenceId = upsertRemote(
                        "sentences", sentence.remoteId,
                        RemoteSentenceRow(
                            ownerId = null, topicId = remoteTopicId, text = sentence.text,
                            translations = sentence.translations.joinToString(LIST_SEPARATOR),
                            ruleIds = sentence.ruleIds.toRemoteRuleIds(ruleIdMap),
                        ),
                    )
                    sentenceDao.setRemoteId(sentence.id, remoteSentenceId)
                }

                for (video in videoDao.getForTopic(topic.id)) {
                    if (video.youtubeUrl == null) {
                        skippedVideoNames += video.name // local-file video, can't publish without file storage
                        continue
                    }
                    val remoteVideoId = upsertRemote(
                        "videos", video.remoteId,
                        RemoteVideoRow(
                            ownerId = null, topicId = remoteTopicId, name = video.name, youtubeUrl = video.youtubeUrl,
                            originalText = video.originalText, translationText = video.translationText,
                            ruleIds = video.ruleIds.toRemoteRuleIds(ruleIdMap),
                        ),
                    )
                    videoDao.setRemoteId(video.id, remoteVideoId)

                    for (question in testQuestionDao.getForOwner(QuestionOwnerType.VIDEO, video.id)) {
                        val remoteQuestionId = upsertRemote(
                            "test_questions", question.remoteId,
                            RemoteTestQuestionRow(
                                ownerId = null, videoId = remoteVideoId, questionText = question.questionText,
                                answerType = question.answerType.name, correctBoolean = question.correctBoolean,
                                acceptableAnswers = question.acceptableAnswers.takeIf { it.isNotEmpty() }?.joinToString(LIST_SEPARATOR),
                            ),
                        )
                        testQuestionDao.setRemoteId(question.id, remoteQuestionId)
                    }
                }

                for (story in storyDao.getForTopic(topic.id)) {
                    val remoteStoryId = upsertRemote(
                        "stories", story.remoteId,
                        RemoteStoryRow(
                            ownerId = null, topicId = remoteTopicId, name = story.name, text = story.text,
                            translation = story.translation, ruleIds = story.ruleIds.toRemoteRuleIds(ruleIdMap),
                        ),
                    )
                    storyDao.setRemoteId(story.id, remoteStoryId)
                }

                for (image in imageContentDao.getForTopic(topic.id)) {
                    val imageData = encodeImageFile(image.imagePath) ?: continue // local file missing, can't publish
                    val remoteImageId = upsertRemote(
                        "image_content", image.remoteId,
                        RemoteImageContentRow(ownerId = null, topicId = remoteTopicId, name = image.name, translation = image.translation, imageData = imageData),
                    )
                    imageContentDao.setRemoteId(image.id, remoteImageId)
                }
            }
        }

        return PublishResult(skippedVideoNames)
    }

    /** Admin-authored languages available on the server that this profile hasn't downloaded yet. */
    suspend fun listDownloadableLanguages(): List<DownloadableLanguage> {
        val remote = supabase.from("languages")
            .select(Columns.list("id", "name", "voice_name")) {
                filter { exact("owner_id", null as Boolean?) }
            }
            .decodeList<RemoteLanguageRow>()
        return remote.mapNotNull { row ->
            val id = row.id ?: return@mapNotNull null
            if (languageDao.getByRemoteId(id) != null) return@mapNotNull null // already downloaded
            DownloadableLanguage(id, row.name, row.voiceName)
        }
    }

    /** Downloads one published language (and everything under it) into this profile's own local
     * copy — from then on it behaves exactly like anything created locally, with its own
     * progress that starts fresh at rating 0. */
    suspend fun downloadLanguage(remoteLanguageId: String, profileId: Long): Long {
        val languageRow = supabase.from("languages")
            .select(Columns.list("id", "name", "voice_name")) { filter { eq("id", remoteLanguageId) } }
            .decodeSingle<RemoteLanguageRow>()
        val localLanguageId = languageDao.insert(
            LanguageEntity(profileId = profileId, name = languageRow.name, voiceName = languageRow.voiceName, remoteId = remoteLanguageId),
        )

        val remoteRules = supabase.from("rules")
            .select(Columns.list("id", "name", "text", "image_data")) { filter { eq("language_id", remoteLanguageId) } }
            .decodeList<RemoteRuleRow>()
        val ruleIdMap = mutableMapOf<String, Long>() // remote uuid -> local id
        for (ruleRow in remoteRules) {
            val remoteRuleId = requireNotNull(ruleRow.id)
            val localRuleId = ruleDao.insert(
                RuleEntity(
                    languageId = localLanguageId, name = ruleRow.name, text = ruleRow.text,
                    imagePath = decodeImageToFile(context, ruleRow.imageData, "rule"), remoteId = remoteRuleId,
                ),
            )
            ruleIdMap[remoteRuleId] = localRuleId
        }

        val sections = supabase.from("sections")
            .select(Columns.list("id", "name", "position")) { filter { eq("language_id", remoteLanguageId) } }
            .decodeList<RemoteSectionRow>()
        for (sectionRow in sections) {
            val remoteSectionId = requireNotNull(sectionRow.id)
            val localSectionId = sectionDao.insert(
                SectionEntity(languageId = localLanguageId, name = sectionRow.name, position = sectionRow.position, remoteId = remoteSectionId),
            )

            val topics = supabase.from("topics")
                .select(Columns.list("id", "name", "position")) { filter { eq("section_id", remoteSectionId) } }
                .decodeList<RemoteTopicRow>()
            for (topicRow in topics) {
                val remoteTopicId = requireNotNull(topicRow.id)
                val localTopicId = topicDao.insert(
                    TopicEntity(sectionId = localSectionId, name = topicRow.name, position = topicRow.position, remoteId = remoteTopicId),
                )

                val words = supabase.from("words")
                    .select(Columns.list("id", "term", "translation", "rule_id", "image_data")) { filter { eq("topic_id", remoteTopicId) } }
                    .decodeList<RemoteWordRow>()
                for (wordRow in words) {
                    wordDao.insert(
                        WordEntity(
                            topicId = localTopicId, term = wordRow.term, translation = wordRow.translation,
                            ruleId = wordRow.ruleId?.let { ruleIdMap[it] },
                            imagePath = decodeImageToFile(context, wordRow.imageData, "word"), remoteId = wordRow.id,
                        ),
                    )
                }

                val sentences = supabase.from("sentences")
                    .select(Columns.list("id", "text", "translations", "rule_ids")) { filter { eq("topic_id", remoteTopicId) } }
                    .decodeList<RemoteSentenceRow>()
                for (sentenceRow in sentences) {
                    sentenceDao.insert(
                        SentenceEntity(
                            topicId = localTopicId, text = sentenceRow.text,
                            translations = sentenceRow.translations.split(LIST_SEPARATOR),
                            ruleIds = sentenceRow.ruleIds.toLocalRuleIds(ruleIdMap),
                            remoteId = sentenceRow.id,
                        ),
                    )
                }

                val videos = supabase.from("videos")
                    .select(Columns.list("id", "name", "youtube_url", "original_text", "translation_text", "rule_ids")) {
                        filter { eq("topic_id", remoteTopicId) }
                    }
                    .decodeList<RemoteVideoRow>()
                for (videoRow in videos) {
                    val remoteVideoId = requireNotNull(videoRow.id)
                    val localVideoId = videoDao.insert(
                        VideoEntity(
                            topicId = localTopicId, name = videoRow.name, youtubeUrl = videoRow.youtubeUrl,
                            originalText = videoRow.originalText, translationText = videoRow.translationText,
                            ruleIds = videoRow.ruleIds.toLocalRuleIds(ruleIdMap), remoteId = remoteVideoId,
                        ),
                    )

                    val questions = supabase.from("test_questions")
                        .select(Columns.list("id", "question_text", "answer_type", "correct_boolean", "acceptable_answers")) {
                            filter { eq("video_id", remoteVideoId) }
                        }
                        .decodeList<RemoteTestQuestionRow>()
                    for (questionRow in questions) {
                        testQuestionDao.insert(
                            TestQuestionEntity(
                                ownerType = QuestionOwnerType.VIDEO, ownerId = localVideoId, questionText = questionRow.questionText,
                                answerType = if (questionRow.answerType == AnswerType.TRUE_FALSE.name) AnswerType.TRUE_FALSE else AnswerType.EXACT_TEXT,
                                correctBoolean = questionRow.correctBoolean,
                                acceptableAnswers = questionRow.acceptableAnswers?.split(LIST_SEPARATOR) ?: emptyList(),
                                remoteId = questionRow.id,
                            ),
                        )
                    }
                }

                val stories = supabase.from("stories")
                    .select(Columns.list("id", "name", "text", "translation", "rule_ids")) { filter { eq("topic_id", remoteTopicId) } }
                    .decodeList<RemoteStoryRow>()
                for (storyRow in stories) {
                    storyDao.insert(
                        StoryEntity(
                            topicId = localTopicId, name = storyRow.name, text = storyRow.text, translation = storyRow.translation,
                            ruleIds = storyRow.ruleIds.toLocalRuleIds(ruleIdMap), remoteId = storyRow.id,
                        ),
                    )
                }

                val images = supabase.from("image_content")
                    .select(Columns.list("id", "name", "translation", "image_data")) { filter { eq("topic_id", remoteTopicId) } }
                    .decodeList<RemoteImageContentRow>()
                for (imageRow in images) {
                    val localPath = decodeImageToFile(context, imageRow.imageData, "image") ?: continue
                    imageContentDao.insert(
                        ImageContentEntity(
                            topicId = localTopicId, name = imageRow.name, imagePath = localPath,
                            translation = imageRow.translation, remoteId = imageRow.id,
                        ),
                    )
                }
            }
        }
        return localLanguageId
    }
}
