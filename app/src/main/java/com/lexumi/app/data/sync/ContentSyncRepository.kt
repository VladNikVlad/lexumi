package com.lexumi.app.data.sync

import android.content.Context
import android.util.Base64
import com.lexumi.app.data.auth.AuthRepository
import com.lexumi.app.data.local.dao.AudioDialogDao
import com.lexumi.app.data.local.dao.ImageContentDao
import com.lexumi.app.data.local.dao.LanguageDao
import com.lexumi.app.data.local.dao.RuleDao
import com.lexumi.app.data.local.dao.SectionDao
import com.lexumi.app.data.local.dao.SentenceDao
import com.lexumi.app.data.local.dao.StoryDao
import com.lexumi.app.data.local.dao.TestQuestionDao
import com.lexumi.app.data.local.dao.SentenceTopicCrossRefDao
import com.lexumi.app.data.local.dao.TopicDao
import com.lexumi.app.data.local.dao.VideoDao
import com.lexumi.app.data.local.dao.WordDao
import com.lexumi.app.data.local.dao.WordTopicCrossRefDao
import com.lexumi.app.data.local.entity.AnswerType
import com.lexumi.app.data.local.entity.AudioDialogEntity
import com.lexumi.app.data.local.entity.ImageContentEntity
import com.lexumi.app.data.local.entity.LanguageEntity
import com.lexumi.app.data.local.entity.QuestionOwnerType
import com.lexumi.app.data.local.entity.RuleEntity
import com.lexumi.app.data.local.entity.SectionEntity
import com.lexumi.app.data.local.entity.SentenceEntity
import com.lexumi.app.data.local.entity.SentenceTopicCrossRefEntity
import com.lexumi.app.data.local.entity.StoryEntity
import com.lexumi.app.data.local.entity.TestQuestionEntity
import com.lexumi.app.data.local.entity.TopicEntity
import com.lexumi.app.data.local.entity.VideoEntity
import com.lexumi.app.data.local.entity.WordEntity
import com.lexumi.app.data.local.entity.WordTopicCrossRefEntity
import com.lexumi.app.domain.model.Sentence
import com.lexumi.app.domain.model.Word
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Audio recordings aren't size-capped like images, so they live in a real Supabase Storage
 * bucket (created manually via the dashboard, see backend/SCHEMA.md) rather than a base64 column. */
private const val AUDIO_BUCKET = "audio-dialogs"

// --- rows exactly as they exist on the Supabase side (see backend/SCHEMA.md) ---
// Only the columns this pass actually needs are listed — Postgres fills in every
// other column (rating, streaks, timestamps...) from its own defaults on insert.
// `translations`/`acceptable_answers` are stored as a single text column, joined with the same
// unit-separator () as the local Room `Converters.fromStringList`; `rule_ids` is a plain
// comma-joined list of remote rule uuids (uuids never contain a comma).
//
// The "which language/section/topic this belongs to" fields (languageId/sectionId/topicId)
// default to "" even though the column is NOT NULL on the server: when reading rows back
// (downloadLanguage/refreshLanguage), the parent is already known from the query's own filter,
// so it's deliberately left out of the `select(Columns.list(...))` call — with no default,
// kotlinx.serialization would fail to decode every row ("Field '...' is required"). Writing a
// row (publishX) always passes the real value explicitly, so the default is never actually used
// for an insert/update.

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
    @SerialName("language_id") val languageId: String = "",
    val name: String,
    val position: Int,
)

@Serializable
private data class RemoteTopicRow(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("section_id") val sectionId: String = "",
    val name: String,
    val position: Int,
)

/** Language-scoped and shared across topics — see [WordTopicCrossRefEntity]. A topic's own use of
 * this word (and its translation override, if it forked one) is a separate row, [RemoteTopicWordRow]. */
@Serializable
private data class RemoteWordRow(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("language_id") val languageId: String = "",
    val term: String,
    val translations: String,
    @SerialName("rule_id") val ruleId: String? = null,
    @SerialName("image_data") val imageData: String? = null,
)

@Serializable
private data class RemoteTopicWordRow(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("topic_id") val topicId: String = "",
    @SerialName("word_id") val wordId: String,
    @SerialName("translation_override") val translationOverride: String? = null,
    val position: Int = 0,
)

@Serializable
private data class RemoteRuleRow(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("language_id") val languageId: String = "",
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
    @SerialName("topic_id") val topicId: String = "",
    val name: String,
    val translation: String,
    @SerialName("image_data") val imageData: String,
)

/** Language-scoped and shared across topics — see [SentenceTopicCrossRefEntity]. A topic's own use
 * of this sentence (and its translations override, if it forked one) is a separate row,
 * [RemoteTopicSentenceRow]. */
@Serializable
private data class RemoteSentenceRow(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("language_id") val languageId: String = "",
    val text: String,
    val translations: String,
    @SerialName("rule_ids") val ruleIds: String? = null,
)

@Serializable
private data class RemoteTopicSentenceRow(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("topic_id") val topicId: String = "",
    @SerialName("sentence_id") val sentenceId: String,
    @SerialName("translations_override") val translationsOverride: String? = null,
    val position: Int = 0,
)

/** Videos are YouTube-link-only going forward — a locally-hosted file can't be published without
 * file storage, so `youtubeUrl` is required here (unlike the local `VideoEntity`, where it's still
 * nullable for old local-file rows that never get published). */
@Serializable
private data class RemoteVideoRow(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("topic_id") val topicId: String = "",
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
    @SerialName("topic_id") val topicId: String = "",
    val name: String,
    val text: String,
    val translation: String? = null,
    @SerialName("rule_ids") val ruleIds: String? = null,
)

/** Belongs to exactly one of [videoId] / [audioDialogId] — mirrors the local entity's polymorphic
 * `(ownerType, ownerId)` pair as two nullable FK columns instead. */
@Serializable
private data class RemoteTestQuestionRow(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("video_id") val videoId: String? = null,
    @SerialName("audio_dialog_id") val audioDialogId: String? = null,
    @SerialName("question_text") val questionText: String,
    @SerialName("answer_type") val answerType: String,
    @SerialName("correct_boolean") val correctBoolean: Boolean? = null,
    @SerialName("acceptable_answers") val acceptableAnswers: String? = null,
)

/** The Postgres row is just metadata — the audio bytes themselves live in Supabase Storage
 * (bucket [AUDIO_BUCKET]) at the path stored here. */
@Serializable
private data class RemoteAudioDialogRow(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("topic_id") val topicId: String = "",
    val name: String,
    @SerialName("translation_text") val translationText: String? = null,
    @SerialName("rule_ids") val ruleIds: String? = null,
    @SerialName("audio_path") val audioPath: String,
)

/** Per-user progress, kept OFF the shared `words` row (that row is one global admin-authored
 * record shared by every user who studies it — writing rating/streaks onto it directly would mean
 * every user overwrites every other user's progress). `word_id`/`user_id` together are unique on
 * the server; `id`/`userId` are omitted here since they're never needed client-side (RLS already
 * scopes every read/write to `auth.uid()`, and upserts key on `(user_id, word_id)`). */
@Serializable
private data class RemoteWordProgressRow(
    @SerialName("word_id") val wordId: String,
    val rating: Int = 0,
    @SerialName("correct_streak") val correctStreak: Int = 0,
    @SerialName("typed_streak") val typedStreak: Int = 0,
    @SerialName("typed_reverse_active") val typedReverseActive: Boolean = false,
    @SerialName("voice_streak") val voiceStreak: Int = 0,
    @SerialName("final_streak") val finalStreak: Int = 0,
    @SerialName("times_seen") val timesSeen: Int = 0,
    @SerialName("in_review_list") val inReviewList: Boolean = false,
    @SerialName("total_correct") val totalCorrect: Int = 0,
    @SerialName("best_streak") val bestStreak: Int = 0,
    @SerialName("current_stats_streak") val currentStatsStreak: Int = 0,
)

/** Mirrors [RemoteWordProgressRow] for sentences — see its doc comment. */
@Serializable
private data class RemoteSentenceProgressRow(
    @SerialName("sentence_id") val sentenceId: String,
    val rating: Int = 0,
    @SerialName("direct_streak") val directStreak: Int = 0,
    @SerialName("reverse_streak") val reverseStreak: Int = 0,
    @SerialName("audio_streak") val audioStreak: Int = 0,
    @SerialName("voice_streak") val voiceStreak: Int = 0,
    @SerialName("times_seen") val timesSeen: Int = 0,
    @SerialName("total_correct") val totalCorrect: Int = 0,
    @SerialName("best_streak") val bestStreak: Int = 0,
    @SerialName("current_stats_streak") val currentStatsStreak: Int = 0,
    val known: Boolean = false,
)

/** [RemoteWordProgressRow] plus `user_id` — needed only when WRITING a row (the read side relies
 * on RLS to already scope every SELECT to `auth.uid()`, so it never needs to decode this field). */
@Serializable
private data class RemoteWordProgressUpsert(
    @SerialName("user_id") val userId: String,
    @SerialName("word_id") val wordId: String,
    val rating: Int,
    @SerialName("correct_streak") val correctStreak: Int,
    @SerialName("typed_streak") val typedStreak: Int,
    @SerialName("typed_reverse_active") val typedReverseActive: Boolean,
    @SerialName("voice_streak") val voiceStreak: Int,
    @SerialName("final_streak") val finalStreak: Int,
    @SerialName("times_seen") val timesSeen: Int,
    @SerialName("in_review_list") val inReviewList: Boolean,
    @SerialName("total_correct") val totalCorrect: Int,
    @SerialName("best_streak") val bestStreak: Int,
    @SerialName("current_stats_streak") val currentStatsStreak: Int,
)

/** Mirrors [RemoteWordProgressUpsert] for sentences. */
@Serializable
private data class RemoteSentenceProgressUpsert(
    @SerialName("user_id") val userId: String,
    @SerialName("sentence_id") val sentenceId: String,
    val rating: Int,
    @SerialName("direct_streak") val directStreak: Int,
    @SerialName("reverse_streak") val reverseStreak: Int,
    @SerialName("audio_streak") val audioStreak: Int,
    @SerialName("voice_streak") val voiceStreak: Int,
    @SerialName("times_seen") val timesSeen: Int,
    @SerialName("total_correct") val totalCorrect: Int,
    @SerialName("best_streak") val bestStreak: Int,
    @SerialName("current_stats_streak") val currentStatsStreak: Int,
    val known: Boolean,
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
    return saveBytesToFile(context, bytes, prefix, "jpg")
}

private fun saveBytesToFile(context: Context, bytes: ByteArray, prefix: String, extension: String): String {
    val file = File(context.filesDir, "${prefix}_${System.currentTimeMillis()}_${(0..999999).random()}.$extension")
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
 * Covers languages/sections/topics/words/rules/sentences/videos(YouTube-only)/stories/image cards/
 * audio dialogues, and the test questions attached to a video or audio dialogue. Word/rule/
 * image-card images are small (<=100kb, enforced by `util/ImageCompressor.kt`) so they're
 * embedded as base64 text directly in their row. Audio recordings aren't size-capped the same
 * way, so they're uploaded as real files to a Supabase Storage bucket ([AUDIO_BUCKET]) instead —
 * the Postgres `audio_dialogs` row only carries the object's path in that bucket.
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
    private val wordTopicCrossRefDao: WordTopicCrossRefDao,
    private val ruleDao: RuleDao,
    private val sentenceDao: SentenceDao,
    private val sentenceTopicCrossRefDao: SentenceTopicCrossRefDao,
    private val videoDao: VideoDao,
    private val storyDao: StoryDao,
    private val testQuestionDao: TestQuestionDao,
    private val imageContentDao: ImageContentDao,
    private val audioDialogDao: AudioDialogDao,
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

    /** Inserts every row in [rows] in ONE request (Postgrest's bulk-insert endpoint) instead of
     * one round trip each — the ids come back in the same order as [rows] (a multi-row
     * `INSERT ... VALUES ... RETURNING` preserves input order in Postgres). */
    private suspend inline fun <reified T : Any> batchInsertRemote(table: String, rows: List<T>): List<String> =
        if (rows.isEmpty()) emptyList()
        else supabase.from(table).insert(rows) { select(Columns.list("id")) }.decodeList<InsertedId>().map { it.id }

    /** Publishes every item in [items]: already-published ones (non-null [remoteId]) are updated
     * one at a time — a re-publish/edit, comparatively rare — while brand-new ones are inserted
     * together in a single batched request via [batchInsertRemote]. That second case is the
     * common one for a first-time publish, where doing it one row at a time would otherwise mean
     * hundreds of sequential round trips for what's normally a small amount of actual data.
     * Returns each item's local id mapped to its resulting remote id. */
    private suspend inline fun <T, reified R : Any> publishAll(
        table: String,
        items: List<T>,
        localId: (T) -> Long,
        remoteId: (T) -> String?,
        setRemoteId: suspend (Long, String) -> Unit,
        buildRow: (T) -> R,
    ): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        val (published, fresh) = items.partition { remoteId(it) != null }
        for (item in published) {
            val id = upsertRemote(table, remoteId(item), buildRow(item))
            setRemoteId(localId(item), id)
            result[localId(item)] = id
        }
        if (fresh.isNotEmpty()) {
            val ids = batchInsertRemote(table, fresh.map(buildRow))
            fresh.zip(ids).forEach { (item, id) -> setRemoteId(localId(item), id); result[localId(item)] = id }
        }
        return result
    }

    /** Pushes this language and everything under it up to Supabase. Safe to call again later —
     * already-published rows get their remote copy refreshed (not duplicated) rather than
     * skipped, so a later edit or a newly-added image reaches Supabase too. Throws if the
     * signed-in user isn't an admin (also enforced server-side by Row Level Security either way).
     *
     * Each entity type's [publishAll] call lives in its own small `private suspend fun` below
     * rather than inline right here — [publishAll] is itself `inline` (needed for `reified` JSON
     * (de)serialization), and a dozen inlined copies of it back-to-back in one method blew past
     * the JVM's 64KB bytecode-per-method limit (`MethodTooLargeException`). Splitting each call
     * site into its own method keeps every individual method's inlined bytecode small, while this
     * one just makes ordinary (non-inlined) suspend calls to them. */
    suspend fun publishLanguage(languageId: Long): PublishResult {
        val profile = authRepository.getMyProfile()
        check(profile?.isAdmin == true) { "Публікувати може лише адмін" }

        val language = languageDao.getById(languageId) ?: return PublishResult(emptyList())
        val remoteLanguageId = upsertRemote(
            "languages", language.remoteId,
            RemoteLanguageRow(ownerId = null, name = language.name, voiceName = language.voiceName),
        )
        languageDao.setRemoteId(language.id, remoteLanguageId)

        // Rules/words/sentences are language-scoped (shared across topics), so they're published
        // once per language here, then linked from each topic below.
        val ruleIdMap = publishRules(languageId, remoteLanguageId)
        val wordIdMap = publishWords(languageId, remoteLanguageId, ruleIdMap)
        val sentenceIdMap = publishSentences(languageId, remoteLanguageId, ruleIdMap)

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

                publishTopicWords(topic.id, remoteTopicId, wordIdMap)
                publishTopicSentences(topic.id, remoteTopicId, sentenceIdMap)
                skippedVideoNames += publishVideos(topic.id, remoteTopicId, ruleIdMap)
                publishStories(topic.id, remoteTopicId, ruleIdMap)
                publishImages(topic.id, remoteTopicId)
                publishAudioDialogs(topic.id, remoteTopicId, ruleIdMap)
            }
        }

        return PublishResult(skippedVideoNames)
    }

    private suspend fun publishRules(languageId: Long, remoteLanguageId: String): Map<Long, String> =
        publishAll(
            table = "rules", items = ruleDao.getForLanguage(languageId),
            localId = { it.id }, remoteId = { it.remoteId }, setRemoteId = ruleDao::setRemoteId,
            buildRow = { rule ->
                RemoteRuleRow(
                    ownerId = null, languageId = remoteLanguageId, name = rule.name, text = rule.text,
                    imageData = encodeImageFile(rule.imagePath),
                )
            },
        )

    private suspend fun publishWords(languageId: Long, remoteLanguageId: String, ruleIdMap: Map<Long, String>): Map<Long, String> =
        publishAll(
            table = "words", items = wordDao.getForLanguage(languageId),
            localId = { it.id }, remoteId = { it.remoteId }, setRemoteId = wordDao::setRemoteId,
            buildRow = { word ->
                RemoteWordRow(
                    ownerId = null, languageId = remoteLanguageId, term = word.term,
                    translations = word.translations.joinToString(LIST_SEPARATOR),
                    ruleId = word.ruleId?.let { ruleIdMap[it] }, imageData = encodeImageFile(word.imagePath),
                )
            },
        )

    private suspend fun publishSentences(languageId: Long, remoteLanguageId: String, ruleIdMap: Map<Long, String>): Map<Long, String> =
        publishAll(
            table = "sentences", items = sentenceDao.getForLanguage(languageId),
            localId = { it.id }, remoteId = { it.remoteId }, setRemoteId = sentenceDao::setRemoteId,
            buildRow = { sentence ->
                RemoteSentenceRow(
                    ownerId = null, languageId = remoteLanguageId, text = sentence.text,
                    translations = sentence.translations.joinToString(LIST_SEPARATOR),
                    ruleIds = sentence.ruleIds.toRemoteRuleIds(ruleIdMap),
                )
            },
        )

    private suspend fun publishTopicWords(topicId: Long, remoteTopicId: String, wordIdMap: Map<Long, String>) {
        val links = wordTopicCrossRefDao.getForTopic(topicId).filter { it.wordId in wordIdMap }
        publishAll(
            table = "topic_words", items = links,
            localId = { it.id }, remoteId = { it.remoteId }, setRemoteId = wordTopicCrossRefDao::setRemoteId,
            buildRow = { link ->
                RemoteTopicWordRow(
                    ownerId = null, topicId = remoteTopicId, wordId = wordIdMap.getValue(link.wordId),
                    translationOverride = link.translationOverride, position = link.position,
                )
            },
        )
    }

    private suspend fun publishTopicSentences(topicId: Long, remoteTopicId: String, sentenceIdMap: Map<Long, String>) {
        val links = sentenceTopicCrossRefDao.getForTopic(topicId).filter { it.sentenceId in sentenceIdMap }
        publishAll(
            table = "topic_sentences", items = links,
            localId = { it.id }, remoteId = { it.remoteId }, setRemoteId = sentenceTopicCrossRefDao::setRemoteId,
            buildRow = { link ->
                RemoteTopicSentenceRow(
                    ownerId = null, topicId = remoteTopicId, sentenceId = sentenceIdMap.getValue(link.sentenceId),
                    translationsOverride = link.translationsOverride?.joinToString(LIST_SEPARATOR), position = link.position,
                )
            },
        )
    }

    /** Publishes every video with a YouTube link (plus its test questions) and returns the names
     * of any local-file videos that were skipped instead — can't publish those without file storage. */
    private suspend fun publishVideos(topicId: Long, remoteTopicId: String, ruleIdMap: Map<Long, String>): List<String> {
        val allVideos = videoDao.getForTopic(topicId)
        val publishableVideos = allVideos.filter { it.youtubeUrl != null }
        val videoIdMap = publishAll(
            table = "videos", items = publishableVideos,
            localId = { it.id }, remoteId = { it.remoteId }, setRemoteId = videoDao::setRemoteId,
            buildRow = { video ->
                RemoteVideoRow(
                    ownerId = null, topicId = remoteTopicId, name = video.name, youtubeUrl = video.youtubeUrl!!,
                    originalText = video.originalText, translationText = video.translationText,
                    ruleIds = video.ruleIds.toRemoteRuleIds(ruleIdMap),
                )
            },
        )
        for (video in publishableVideos) {
            val remoteVideoId = videoIdMap[video.id] ?: continue
            publishAll(
                table = "test_questions", items = testQuestionDao.getForOwner(QuestionOwnerType.VIDEO, video.id),
                localId = { it.id }, remoteId = { it.remoteId }, setRemoteId = testQuestionDao::setRemoteId,
                buildRow = { question ->
                    RemoteTestQuestionRow(
                        ownerId = null, videoId = remoteVideoId, questionText = question.questionText,
                        answerType = question.answerType.name, correctBoolean = question.correctBoolean,
                        acceptableAnswers = question.acceptableAnswers.takeIf { it.isNotEmpty() }?.joinToString(LIST_SEPARATOR),
                    )
                },
            )
        }
        return allVideos.filter { it.youtubeUrl == null }.map { it.name }
    }

    private suspend fun publishStories(topicId: Long, remoteTopicId: String, ruleIdMap: Map<Long, String>) {
        publishAll(
            table = "stories", items = storyDao.getForTopic(topicId),
            localId = { it.id }, remoteId = { it.remoteId }, setRemoteId = storyDao::setRemoteId,
            buildRow = { story ->
                RemoteStoryRow(
                    ownerId = null, topicId = remoteTopicId, name = story.name, text = story.text,
                    translation = story.translation, ruleIds = story.ruleIds.toRemoteRuleIds(ruleIdMap),
                )
            },
        )
    }

    private suspend fun publishImages(topicId: Long, remoteTopicId: String) {
        // Pre-encode once per image (not per network call) — items missing their local file are dropped.
        val publishableImages = imageContentDao.getForTopic(topicId).mapNotNull { image ->
            encodeImageFile(image.imagePath)?.let { image to it }
        }
        publishAll(
            table = "image_content", items = publishableImages,
            localId = { it.first.id }, remoteId = { it.first.remoteId },
            setRemoteId = imageContentDao::setRemoteId,
            buildRow = { (image, imageData) ->
                RemoteImageContentRow(ownerId = null, topicId = remoteTopicId, name = image.name, translation = image.translation, imageData = imageData)
            },
        )
    }

    /** Audio files get uploaded to Storage per-dialog as a side effect of publishing their row, so
     * (unlike the other entity types) this stays a plain per-row loop rather than [publishAll]. */
    private suspend fun publishAudioDialogs(topicId: Long, remoteTopicId: String, ruleIdMap: Map<Long, String>) {
        for (dialog in audioDialogDao.getForTopic(topicId)) {
            // Keyed by the LOCAL id (always known upfront) rather than the remote row's id
            // (which doesn't exist yet on first publish) — avoids a chicken-and-egg problem.
            val storagePath = "dialogs/${dialog.id}.mp3"
            val remoteDialogId = upsertRemote(
                "audio_dialogs", dialog.remoteId,
                RemoteAudioDialogRow(
                    ownerId = null, topicId = remoteTopicId, name = dialog.name,
                    translationText = dialog.translationText, ruleIds = dialog.ruleIds.toRemoteRuleIds(ruleIdMap),
                    audioPath = storagePath,
                ),
            )
            audioDialogDao.setRemoteId(dialog.id, remoteDialogId)

            val bytes = File(dialog.audioPath).takeIf { it.exists() }?.readBytes()
            if (bytes != null) {
                val bucket = supabase.storage.from(AUDIO_BUCKET)
                if (dialog.remoteId != null) bucket.update(storagePath, bytes) else bucket.upload(storagePath, bytes)
            }

            for (question in testQuestionDao.getForOwner(QuestionOwnerType.AUDIO_DIALOG, dialog.id)) {
                val remoteQuestionId = upsertRemote(
                    "test_questions", question.remoteId,
                    RemoteTestQuestionRow(
                        ownerId = null, audioDialogId = remoteDialogId, questionText = question.questionText,
                        answerType = question.answerType.name, correctBoolean = question.correctBoolean,
                        acceptableAnswers = question.acceptableAnswers.takeIf { it.isNotEmpty() }?.joinToString(LIST_SEPARATOR),
                    ),
                )
                testQuestionDao.setRemoteId(question.id, remoteQuestionId)
            }
        }
    }

    /** Admin-authored languages available on the server that [profileId] specifically hasn't
     * downloaded yet — scoped per profile so a language another account already grabbed still
     * shows up here for this one (each account's local copy is independent from that point on). */
    suspend fun listDownloadableLanguages(profileId: Long): List<DownloadableLanguage> {
        val remote = supabase.from("languages")
            .select(Columns.list("id", "name", "voice_name")) {
                filter { exact("owner_id", null as Boolean?) }
            }
            .decodeList<RemoteLanguageRow>()
        return remote.mapNotNull { row ->
            val id = row.id ?: return@mapNotNull null
            if (languageDao.getByRemoteIdForProfile(id, profileId) != null) return@mapNotNull null // already downloaded
            DownloadableLanguage(id, row.name, row.voiceName)
        }
    }

    /** Downloads one published language's STRUCTURE (sections/topics — names, positions) into this
     * profile's own local copy — deliberately NOT its topics' content (words/videos/etc.), which is
     * what made this slow for a content-heavy language before. A topic's content is synced on
     * demand by [syncTopicContent] the moment it's actually opened, so adding a language stays fast
     * regardless of how much is published under it. */
    suspend fun downloadLanguage(remoteLanguageId: String, profileId: Long): Long {
        val languageRow = supabase.from("languages")
            .select(Columns.list("id", "name", "voice_name")) { filter { eq("id", remoteLanguageId) } }
            .decodeSingle<RemoteLanguageRow>()
        val localLanguageId = languageDao.insert(
            LanguageEntity(profileId = profileId, name = languageRow.name, voiceName = languageRow.voiceName, remoteId = remoteLanguageId),
        )

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
                topicDao.insert(
                    TopicEntity(sectionId = localSectionId, name = topicRow.name, position = topicRow.position, remoteId = requireNotNull(topicRow.id)),
                )
            }
        }
        return localLanguageId
    }

    /** Pulls the latest server state of an already-downloaded/published language into the
     * existing local copy — anyone with a `remoteId`-linked language can use this, not just the
     * admin who originally published it (e.g. content added later through the admin web panel is
     * otherwise invisible locally, since [downloadLanguage] only ever runs once). Rows that
     * already exist locally (matched by `remoteId`) get their content fields updated in place —
     * `rating`/streak/progress fields on words and sentences are never touched, and no row's
     * local `id` ever changes (topics/sections are FK parents with `ON DELETE CASCADE`, so
     * replacing them instead of updating them would wipe everything nested under them).
     *
     * Deliberately one-directional in the safe direction: nothing gets deleted locally just
     * because it disappeared remotely, even for the "leaf" join tables (`topic_words` etc.) that
     * get wiped and reinserted per topic — those hold no progress of their own, only a stale
     * `translationOverride`, so it's safe to always replace them wholesale, but the *rows they
     * point at* (words/sentences/...) are never removed by this pass. See the plan notes for why
     * (a bug here could otherwise cascade-delete real user progress).
     *
     * Split into small `private suspend fun`s per entity for the same reason [publishLanguage] is
     * (readability for an otherwise very long method) — this one isn't `inline`, so it doesn't
     * have that method's bytecode-size risk, but the same shape reads better. */
    suspend fun refreshLanguage(localLanguageId: Long) {
        val language = languageDao.getById(localLanguageId) ?: return
        val remoteLanguageId = requireNotNull(language.remoteId) { "Ця мова ще не пов'язана з сервером" }

        val languageRow = supabase.from("languages")
            .select(Columns.list("id", "name", "voice_name")) { filter { eq("id", remoteLanguageId) } }
            .decodeSingle<RemoteLanguageRow>()
        languageDao.update(language.copy(name = languageRow.name, voiceName = languageRow.voiceName))

        val ruleIdMap = refreshRules(localLanguageId, remoteLanguageId)
        val wordIdMap = refreshWords(localLanguageId, remoteLanguageId, ruleIdMap)
        val sentenceIdMap = refreshSentences(localLanguageId, remoteLanguageId, ruleIdMap)

        val sections = supabase.from("sections")
            .select(Columns.list("id", "name", "position")) { filter { eq("language_id", remoteLanguageId) } }
            .decodeList<RemoteSectionRow>()
        for (sectionRow in sections) {
            val remoteSectionId = requireNotNull(sectionRow.id)
            val existingSection = sectionDao.getByRemoteId(remoteSectionId)
            val localSectionId = if (existingSection != null) {
                sectionDao.update(existingSection.copy(name = sectionRow.name, position = sectionRow.position))
                existingSection.id
            } else {
                sectionDao.insert(SectionEntity(languageId = localLanguageId, name = sectionRow.name, position = sectionRow.position, remoteId = remoteSectionId))
            }

            val topics = supabase.from("topics")
                .select(Columns.list("id", "name", "position")) { filter { eq("section_id", remoteSectionId) } }
                .decodeList<RemoteTopicRow>()
            for (topicRow in topics) {
                val remoteTopicId = requireNotNull(topicRow.id)
                val existingTopic = topicDao.getByRemoteId(remoteTopicId)
                val localTopicId = if (existingTopic != null) {
                    topicDao.update(existingTopic.copy(name = topicRow.name, position = topicRow.position))
                    existingTopic.id
                } else {
                    topicDao.insert(TopicEntity(sectionId = localSectionId, name = topicRow.name, position = topicRow.position, remoteId = remoteTopicId))
                }

                refreshTopicWords(localTopicId, remoteTopicId, wordIdMap)
                refreshTopicSentences(localTopicId, remoteTopicId, sentenceIdMap)
                refreshVideos(localTopicId, remoteTopicId, ruleIdMap)
                refreshStories(localTopicId, remoteTopicId, ruleIdMap)
                refreshImages(localTopicId, remoteTopicId)
                refreshAudioDialogs(localTopicId, remoteTopicId, ruleIdMap)
            }
            // A whole topic removed server-side (e.g. via the admin web panel) — Room's
            // ON DELETE CASCADE from topics to word_topic_cross_ref/sentence_topic_cross_ref/
            // videos/stories/image_content/audio_dialogs takes care of everything nested under
            // it (test_questions has no FK, per its own doc comment, so those specific rows are
            // left as harmless orphans — never reachable again, but not a visible bug).
            val remoteTopicIds = topics.mapNotNull { it.id }.toSet()
            for (localTopic in topicDao.getForSection(localSectionId)) {
                if (localTopic.remoteId != null && localTopic.remoteId !in remoteTopicIds) topicDao.delete(localTopic)
            }
        }
        val remoteSectionIds = sections.mapNotNull { it.id }.toSet()
        for (localSection in sectionDao.getForLanguage(localLanguageId)) {
            if (localSection.remoteId != null && localSection.remoteId !in remoteSectionIds) sectionDao.delete(localSection)
        }
    }

    /** Upserts one already-fetched rule row by remoteId, returning its local id — shared by the
     * full-language [refreshRules] and the single-topic [syncTopicContent], so both get the exact
     * same merge behavior (content updated in place, no new local id) without duplicating it. */
    private suspend fun upsertRuleRow(localLanguageId: Long, ruleRow: RemoteRuleRow): Long {
        val remoteRuleId = requireNotNull(ruleRow.id)
        val existing = ruleDao.getByRemoteId(remoteRuleId)
        val imagePath = decodeImageToFile(context, ruleRow.imageData, "rule")
        return if (existing != null) {
            ruleDao.update(existing.copy(name = ruleRow.name, text = ruleRow.text, imagePath = imagePath))
            existing.id
        } else {
            ruleDao.insert(RuleEntity(languageId = localLanguageId, name = ruleRow.name, text = ruleRow.text, imagePath = imagePath, remoteId = remoteRuleId))
        }
    }

    /** Mirrors [upsertRuleRow] for words — content only, rating/streaks/timesSeen etc. are
     * deliberately untouched (see [pullProgress] for how those actually get updated). */
    private suspend fun upsertWordRow(localLanguageId: Long, wordRow: RemoteWordRow, ruleIdMap: Map<String, Long>): Long {
        val remoteWordId = requireNotNull(wordRow.id)
        val existing = wordDao.getByRemoteId(remoteWordId)
        val translations = wordRow.translations.split(LIST_SEPARATOR)
        val ruleId = wordRow.ruleId?.let { ruleIdMap[it] }
        val imagePath = decodeImageToFile(context, wordRow.imageData, "word")
        return if (existing != null) {
            wordDao.update(existing.copy(term = wordRow.term, translations = translations, ruleId = ruleId, imagePath = imagePath))
            existing.id
        } else {
            wordDao.insert(
                WordEntity(languageId = localLanguageId, term = wordRow.term, translations = translations, ruleId = ruleId, imagePath = imagePath, remoteId = remoteWordId),
            )
        }
    }

    /** Mirrors [upsertRuleRow] for sentences. */
    private suspend fun upsertSentenceRow(localLanguageId: Long, sentenceRow: RemoteSentenceRow, ruleIdMap: Map<String, Long>): Long {
        val remoteSentenceId = requireNotNull(sentenceRow.id)
        val existing = sentenceDao.getByRemoteId(remoteSentenceId)
        val translations = sentenceRow.translations.split(LIST_SEPARATOR)
        val ruleIds = sentenceRow.ruleIds.toLocalRuleIds(ruleIdMap)
        return if (existing != null) {
            sentenceDao.update(existing.copy(text = sentenceRow.text, translations = translations, ruleIds = ruleIds))
            existing.id
        } else {
            sentenceDao.insert(SentenceEntity(languageId = localLanguageId, text = sentenceRow.text, translations = translations, ruleIds = ruleIds, remoteId = remoteSentenceId))
        }
    }

    private suspend fun refreshRules(localLanguageId: Long, remoteLanguageId: String): Map<String, Long> {
        val remoteRules = supabase.from("rules")
            .select(Columns.list("id", "name", "text", "image_data")) { filter { eq("language_id", remoteLanguageId) } }
            .decodeList<RemoteRuleRow>()
        val ruleIdMap = mutableMapOf<String, Long>()
        for (ruleRow in remoteRules) {
            ruleIdMap[requireNotNull(ruleRow.id)] = upsertRuleRow(localLanguageId, ruleRow)
        }
        val remoteRuleIds = remoteRules.mapNotNull { it.id }.toSet()
        for (localRule in ruleDao.getForLanguage(localLanguageId)) {
            if (localRule.remoteId != null && localRule.remoteId !in remoteRuleIds) ruleDao.delete(localRule)
        }
        return ruleIdMap
    }

    private suspend fun refreshWords(localLanguageId: Long, remoteLanguageId: String, ruleIdMap: Map<String, Long>): Map<String, Long> {
        val remoteWords = supabase.from("words")
            .select(Columns.list("id", "term", "translations", "rule_id", "image_data")) { filter { eq("language_id", remoteLanguageId) } }
            .decodeList<RemoteWordRow>()
        val wordIdMap = mutableMapOf<String, Long>()
        for (wordRow in remoteWords) {
            wordIdMap[requireNotNull(wordRow.id)] = upsertWordRow(localLanguageId, wordRow, ruleIdMap)
        }
        val remoteWordIds = remoteWords.mapNotNull { it.id }.toSet()
        for (localWord in wordDao.getForLanguage(localLanguageId)) {
            if (localWord.remoteId != null && localWord.remoteId !in remoteWordIds) wordDao.deleteById(localWord.id)
        }
        return wordIdMap
    }

    private suspend fun refreshSentences(localLanguageId: Long, remoteLanguageId: String, ruleIdMap: Map<String, Long>): Map<String, Long> {
        val remoteSentences = supabase.from("sentences")
            .select(Columns.list("id", "text", "translations", "rule_ids")) { filter { eq("language_id", remoteLanguageId) } }
            .decodeList<RemoteSentenceRow>()
        val sentenceIdMap = mutableMapOf<String, Long>()
        for (sentenceRow in remoteSentences) {
            sentenceIdMap[requireNotNull(sentenceRow.id)] = upsertSentenceRow(localLanguageId, sentenceRow, ruleIdMap)
        }
        val remoteSentenceIds = remoteSentences.mapNotNull { it.id }.toSet()
        for (localSentence in sentenceDao.getForLanguage(localLanguageId)) {
            if (localSentence.remoteId != null && localSentence.remoteId !in remoteSentenceIds) sentenceDao.deleteById(localSentence.id)
        }
        return sentenceIdMap
    }

    private suspend fun refreshTopicWords(localTopicId: Long, remoteTopicId: String, wordIdMap: Map<String, Long>) {
        val topicWords = supabase.from("topic_words")
            .select(Columns.list("id", "word_id", "translation_override", "position")) { filter { eq("topic_id", remoteTopicId) } }
            .decodeList<RemoteTopicWordRow>()
        wordTopicCrossRefDao.deleteAllForTopic(localTopicId)
        for (linkRow in topicWords) {
            val localWordId = wordIdMap[linkRow.wordId] ?: continue
            wordTopicCrossRefDao.insert(
                WordTopicCrossRefEntity(
                    topicId = localTopicId, wordId = localWordId, translationOverride = linkRow.translationOverride,
                    position = linkRow.position, remoteId = requireNotNull(linkRow.id),
                ),
            )
        }
    }

    private suspend fun refreshTopicSentences(localTopicId: Long, remoteTopicId: String, sentenceIdMap: Map<String, Long>) {
        val topicSentences = supabase.from("topic_sentences")
            .select(Columns.list("id", "sentence_id", "translations_override", "position")) { filter { eq("topic_id", remoteTopicId) } }
            .decodeList<RemoteTopicSentenceRow>()
        sentenceTopicCrossRefDao.deleteAllForTopic(localTopicId)
        for (linkRow in topicSentences) {
            val localSentenceId = sentenceIdMap[linkRow.sentenceId] ?: continue
            sentenceTopicCrossRefDao.insert(
                SentenceTopicCrossRefEntity(
                    topicId = localTopicId, sentenceId = localSentenceId,
                    translationsOverride = linkRow.translationsOverride?.split(LIST_SEPARATOR),
                    position = linkRow.position, remoteId = requireNotNull(linkRow.id),
                ),
            )
        }
    }

    private suspend fun refreshVideos(localTopicId: Long, remoteTopicId: String, ruleIdMap: Map<String, Long>) {
        val videos = supabase.from("videos")
            .select(Columns.list("id", "name", "youtube_url", "original_text", "translation_text", "rule_ids")) {
                filter { eq("topic_id", remoteTopicId) }
            }
            .decodeList<RemoteVideoRow>()
        for (videoRow in videos) {
            val remoteVideoId = requireNotNull(videoRow.id)
            val existing = videoDao.getByRemoteId(remoteVideoId)
            val ruleIds = videoRow.ruleIds.toLocalRuleIds(ruleIdMap)
            val localVideoId = if (existing != null) {
                videoDao.update(
                    existing.copy(
                        name = videoRow.name, youtubeUrl = videoRow.youtubeUrl,
                        originalText = videoRow.originalText, translationText = videoRow.translationText, ruleIds = ruleIds,
                    ),
                )
                existing.id
            } else {
                videoDao.insert(
                    VideoEntity(
                        topicId = localTopicId, name = videoRow.name, youtubeUrl = videoRow.youtubeUrl,
                        originalText = videoRow.originalText, translationText = videoRow.translationText,
                        ruleIds = ruleIds, remoteId = remoteVideoId,
                    ),
                )
            }

            testQuestionDao.deleteAllForOwner(QuestionOwnerType.VIDEO, localVideoId)
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
        val remoteVideoIds = videos.mapNotNull { it.id }.toSet()
        for (localVideo in videoDao.getForTopic(localTopicId)) {
            if (localVideo.remoteId != null && localVideo.remoteId !in remoteVideoIds) {
                // TestQuestionEntity has no FK to video/audio_dialog (polymorphic owner, see its
                // own doc comment) — deleting the video alone would leave its questions orphaned.
                testQuestionDao.deleteAllForOwner(QuestionOwnerType.VIDEO, localVideo.id)
                videoDao.delete(localVideo)
            }
        }
    }

    private suspend fun refreshStories(localTopicId: Long, remoteTopicId: String, ruleIdMap: Map<String, Long>) {
        val stories = supabase.from("stories")
            .select(Columns.list("id", "name", "text", "translation", "rule_ids")) { filter { eq("topic_id", remoteTopicId) } }
            .decodeList<RemoteStoryRow>()
        for (storyRow in stories) {
            val remoteStoryId = requireNotNull(storyRow.id)
            val existing = storyDao.getByRemoteId(remoteStoryId)
            val ruleIds = storyRow.ruleIds.toLocalRuleIds(ruleIdMap)
            if (existing != null) {
                storyDao.update(existing.copy(name = storyRow.name, text = storyRow.text, translation = storyRow.translation, ruleIds = ruleIds))
            } else {
                storyDao.insert(
                    StoryEntity(topicId = localTopicId, name = storyRow.name, text = storyRow.text, translation = storyRow.translation, ruleIds = ruleIds, remoteId = remoteStoryId),
                )
            }
        }
        val remoteStoryIds = stories.mapNotNull { it.id }.toSet()
        for (localStory in storyDao.getForTopic(localTopicId)) {
            if (localStory.remoteId != null && localStory.remoteId !in remoteStoryIds) storyDao.delete(localStory)
        }
    }

    private suspend fun refreshImages(localTopicId: Long, remoteTopicId: String) {
        val images = supabase.from("image_content")
            .select(Columns.list("id", "name", "translation", "image_data")) { filter { eq("topic_id", remoteTopicId) } }
            .decodeList<RemoteImageContentRow>()
        for (imageRow in images) {
            val remoteImageId = requireNotNull(imageRow.id)
            val localPath = decodeImageToFile(context, imageRow.imageData, "image") ?: continue
            val existing = imageContentDao.getByRemoteId(remoteImageId)
            if (existing != null) {
                imageContentDao.update(existing.copy(name = imageRow.name, imagePath = localPath, translation = imageRow.translation))
            } else {
                imageContentDao.insert(
                    ImageContentEntity(topicId = localTopicId, name = imageRow.name, imagePath = localPath, translation = imageRow.translation, remoteId = remoteImageId),
                )
            }
        }
        val remoteImageIds = images.mapNotNull { it.id }.toSet()
        for (localImage in imageContentDao.getForTopic(localTopicId)) {
            if (localImage.remoteId != null && localImage.remoteId !in remoteImageIds) imageContentDao.delete(localImage)
        }
    }

    private suspend fun refreshAudioDialogs(localTopicId: Long, remoteTopicId: String, ruleIdMap: Map<String, Long>) {
        val dialogs = supabase.from("audio_dialogs")
            .select(Columns.list("id", "name", "translation_text", "rule_ids", "audio_path")) { filter { eq("topic_id", remoteTopicId) } }
            .decodeList<RemoteAudioDialogRow>()
        for (dialogRow in dialogs) {
            val remoteDialogId = requireNotNull(dialogRow.id)
            val bytes = runCatching { supabase.storage.from(AUDIO_BUCKET).downloadPublic(dialogRow.audioPath) }.getOrNull()
                ?: continue // couldn't fetch the audio file, leave whatever's local (if anything) alone
            val localAudioPath = saveBytesToFile(context, bytes, "audio", "mp3")
            val ruleIds = dialogRow.ruleIds.toLocalRuleIds(ruleIdMap)
            val existing = audioDialogDao.getByRemoteId(remoteDialogId)
            val localDialogId = if (existing != null) {
                audioDialogDao.update(existing.copy(name = dialogRow.name, audioPath = localAudioPath, translationText = dialogRow.translationText, ruleIds = ruleIds))
                existing.id
            } else {
                audioDialogDao.insert(
                    AudioDialogEntity(topicId = localTopicId, name = dialogRow.name, audioPath = localAudioPath, translationText = dialogRow.translationText, ruleIds = ruleIds, remoteId = remoteDialogId),
                )
            }

            testQuestionDao.deleteAllForOwner(QuestionOwnerType.AUDIO_DIALOG, localDialogId)
            val questions = supabase.from("test_questions")
                .select(Columns.list("id", "question_text", "answer_type", "correct_boolean", "acceptable_answers")) {
                    filter { eq("audio_dialog_id", remoteDialogId) }
                }
                .decodeList<RemoteTestQuestionRow>()
            for (questionRow in questions) {
                testQuestionDao.insert(
                    TestQuestionEntity(
                        ownerType = QuestionOwnerType.AUDIO_DIALOG, ownerId = localDialogId, questionText = questionRow.questionText,
                        answerType = if (questionRow.answerType == AnswerType.TRUE_FALSE.name) AnswerType.TRUE_FALSE else AnswerType.EXACT_TEXT,
                        correctBoolean = questionRow.correctBoolean,
                        acceptableAnswers = questionRow.acceptableAnswers?.split(LIST_SEPARATOR) ?: emptyList(),
                        remoteId = questionRow.id,
                    ),
                )
            }
        }
        val remoteDialogIds = dialogs.mapNotNull { it.id }.toSet()
        for (localDialog in audioDialogDao.getForTopic(localTopicId)) {
            if (localDialog.remoteId != null && localDialog.remoteId !in remoteDialogIds) {
                testQuestionDao.deleteAllForOwner(QuestionOwnerType.AUDIO_DIALOG, localDialog.id)
                audioDialogDao.delete(localDialog)
            }
        }
    }

    // ============================================================================================
    // On-demand per-topic sync ("Duolingo model") — see the plan notes for why this exists: a full
    // downloadLanguage/refreshLanguage walks every topic's content up front (dozens to hundreds of
    // sequential requests even for a small language), which is what made adding/opening a language
    // slow. Below, [downloadLanguage] only ever syncs the navigational structure (instant), and a
    // topic's actual content (words/videos/etc.) is synced by [syncTopicContent] the moment it's
    // actually opened — [syncLanguageStructure] keeps just that structure fresh on every app open,
    // and [evictStaleTopicContent]/[evictOtherTopicsContent] delete a topic's cached content again
    // once it's no longer the one topic kept available offline.
    // ============================================================================================

    /** Keeps just the navigational tree (sections/topics — names, positions, which still exist) in
     * sync — cheap enough to run automatically on every app open (HomeViewModel does), unlike a
     * full [refreshLanguage]. Topic CONTENT is deliberately untouched here; that's
     * [syncTopicContent]'s job, run on demand once a topic is actually opened. Mirrors
     * [refreshLanguage]'s section/topic loop exactly — just without the rules/words/sentences pass
     * before it. */
    suspend fun syncLanguageStructure(localLanguageId: Long) {
        val language = languageDao.getById(localLanguageId) ?: return
        val remoteLanguageId = language.remoteId ?: return

        val languageRow = supabase.from("languages")
            .select(Columns.list("id", "name", "voice_name")) { filter { eq("id", remoteLanguageId) } }
            .decodeSingle<RemoteLanguageRow>()
        languageDao.update(language.copy(name = languageRow.name, voiceName = languageRow.voiceName))

        val sections = supabase.from("sections")
            .select(Columns.list("id", "name", "position")) { filter { eq("language_id", remoteLanguageId) } }
            .decodeList<RemoteSectionRow>()
        for (sectionRow in sections) {
            val remoteSectionId = requireNotNull(sectionRow.id)
            val existingSection = sectionDao.getByRemoteId(remoteSectionId)
            val localSectionId = if (existingSection != null) {
                sectionDao.update(existingSection.copy(name = sectionRow.name, position = sectionRow.position))
                existingSection.id
            } else {
                sectionDao.insert(SectionEntity(languageId = localLanguageId, name = sectionRow.name, position = sectionRow.position, remoteId = remoteSectionId))
            }

            val topics = supabase.from("topics")
                .select(Columns.list("id", "name", "position")) { filter { eq("section_id", remoteSectionId) } }
                .decodeList<RemoteTopicRow>()
            for (topicRow in topics) {
                val remoteTopicId = requireNotNull(topicRow.id)
                val existingTopic = topicDao.getByRemoteId(remoteTopicId)
                if (existingTopic != null) {
                    topicDao.update(existingTopic.copy(name = topicRow.name, position = topicRow.position))
                } else {
                    topicDao.insert(TopicEntity(sectionId = localSectionId, name = topicRow.name, position = topicRow.position, remoteId = remoteTopicId))
                }
            }
            // A whole topic removed server-side — Room's ON DELETE CASCADE takes care of everything
            // nested under it (see refreshLanguage's identical comment for the test_questions caveat).
            val remoteTopicIds = topics.mapNotNull { it.id }.toSet()
            for (localTopic in topicDao.getForSection(localSectionId)) {
                if (localTopic.remoteId != null && localTopic.remoteId !in remoteTopicIds) topicDao.delete(localTopic)
            }
        }
        val remoteSectionIds = sections.mapNotNull { it.id }.toSet()
        for (localSection in sectionDao.getForLanguage(localLanguageId)) {
            if (localSection.remoteId != null && localSection.remoteId !in remoteSectionIds) sectionDao.delete(localSection)
        }
    }

    /** Syncs exactly one admin topic's content — only the words/sentences/rules it actually
     * references (via two `.in("id", ...)` lookups, not the whole language's word bank), plus the
     * videos/stories/images/audio it owns, plus this user's own progress for the words/sentences
     * involved (see [pullProgress]). Safe to call again for an already-synced topic — same
     * upsert-by-remoteId merge [refreshLanguage] uses, just scoped to this one topic. Re-fetching
     * topic_words/videos/etc a second time inside [refreshTopicWords] etc. below is a deliberate,
     * small duplication — reusing those already-correct functions outweighs the extra handful of
     * cheap requests for a single topic (this is NOT the "hundreds of requests" problem, which only
     * ever came from doing this for EVERY topic of a language at once). */
    suspend fun syncTopicContent(localTopicId: Long) {
        val topic = topicDao.getById(localTopicId) ?: return
        val remoteTopicId = topic.remoteId ?: return
        val section = sectionDao.getById(topic.sectionId) ?: return
        val localLanguageId = section.languageId

        val topicWords = supabase.from("topic_words")
            .select(Columns.list("id", "word_id", "translation_override", "position")) { filter { eq("topic_id", remoteTopicId) } }
            .decodeList<RemoteTopicWordRow>()
        val topicSentences = supabase.from("topic_sentences")
            .select(Columns.list("id", "sentence_id", "translations_override", "position")) { filter { eq("topic_id", remoteTopicId) } }
            .decodeList<RemoteTopicSentenceRow>()
        val videos = supabase.from("videos")
            .select(Columns.list("id", "name", "youtube_url", "original_text", "translation_text", "rule_ids")) { filter { eq("topic_id", remoteTopicId) } }
            .decodeList<RemoteVideoRow>()
        val stories = supabase.from("stories")
            .select(Columns.list("id", "name", "text", "translation", "rule_ids")) { filter { eq("topic_id", remoteTopicId) } }
            .decodeList<RemoteStoryRow>()
        val audioDialogs = supabase.from("audio_dialogs")
            .select(Columns.list("id", "name", "translation_text", "rule_ids", "audio_path")) { filter { eq("topic_id", remoteTopicId) } }
            .decodeList<RemoteAudioDialogRow>()

        val wordIds = topicWords.map { it.wordId }.distinct()
        val remoteWords = if (wordIds.isEmpty()) emptyList() else supabase.from("words")
            .select(Columns.list("id", "term", "translations", "rule_id", "image_data")) { filter { isIn("id", wordIds) } }
            .decodeList<RemoteWordRow>()
        val sentenceIds = topicSentences.map { it.sentenceId }.distinct()
        val remoteSentences = if (sentenceIds.isEmpty()) emptyList() else supabase.from("sentences")
            .select(Columns.list("id", "text", "translations", "rule_ids")) { filter { isIn("id", sentenceIds) } }
            .decodeList<RemoteSentenceRow>()

        val ruleIds = (
            remoteWords.mapNotNull { it.ruleId } +
                remoteSentences.flatMap { it.ruleIds?.split(",") ?: emptyList() } +
                videos.flatMap { it.ruleIds?.split(",") ?: emptyList() } +
                stories.flatMap { it.ruleIds?.split(",") ?: emptyList() } +
                audioDialogs.flatMap { it.ruleIds?.split(",") ?: emptyList() }
            ).distinct()
        val remoteRules = if (ruleIds.isEmpty()) emptyList() else supabase.from("rules")
            .select(Columns.list("id", "name", "text", "image_data")) { filter { isIn("id", ruleIds) } }
            .decodeList<RemoteRuleRow>()

        val ruleIdMap = remoteRules.associate { requireNotNull(it.id) to upsertRuleRow(localLanguageId, it) }
        val wordIdMap = remoteWords.associate { requireNotNull(it.id) to upsertWordRow(localLanguageId, it, ruleIdMap) }
        val sentenceIdMap = remoteSentences.associate { requireNotNull(it.id) to upsertSentenceRow(localLanguageId, it, ruleIdMap) }

        refreshTopicWords(localTopicId, remoteTopicId, wordIdMap)
        refreshTopicSentences(localTopicId, remoteTopicId, sentenceIdMap)
        refreshVideos(localTopicId, remoteTopicId, ruleIdMap)
        refreshStories(localTopicId, remoteTopicId, ruleIdMap)
        refreshImages(localTopicId, remoteTopicId)
        refreshAudioDialogs(localTopicId, remoteTopicId, ruleIdMap)

        pullProgress(wordIdMap, sentenceIdMap)
    }

    /** Pulls this user's own rating/streak fields for the words/sentences just synced by
     * [syncTopicContent] from `word_progress`/`sentence_progress` (see those tables' own doc
     * comments in backend/word_sentence_progress.sql for why progress can't live on the shared
     * admin row) and applies them to the local rows. A word/sentence with no progress row yet just
     * keeps its freshly-inserted defaults (rating 0) — same as any word never studied before.
     * Best-effort: a failure here (e.g. offline mid-sync) leaves progress at whatever it locally
     * already was rather than blocking the content sync that already succeeded above. */
    private suspend fun pullProgress(wordIdMap: Map<String, Long>, sentenceIdMap: Map<String, Long>) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        if (wordIdMap.isNotEmpty()) {
            val rows = runCatching {
                supabase.from("word_progress")
                    .select(
                        Columns.list(
                            "word_id", "rating", "correct_streak", "typed_streak", "typed_reverse_active",
                            "voice_streak", "final_streak", "times_seen", "in_review_list", "total_correct",
                            "best_streak", "current_stats_streak",
                        ),
                    ) { filter { eq("user_id", userId); isIn("word_id", wordIdMap.keys.toList()) } }
                    .decodeList<RemoteWordProgressRow>()
            }.getOrDefault(emptyList())
            for (row in rows) {
                val localId = wordIdMap[row.wordId] ?: continue
                val existing = wordDao.getById(localId) ?: continue
                wordDao.update(
                    existing.copy(
                        rating = row.rating, correctStreak = row.correctStreak, typedStreak = row.typedStreak,
                        typedReverseActive = row.typedReverseActive, voiceStreak = row.voiceStreak, finalStreak = row.finalStreak,
                        timesSeen = row.timesSeen, inReviewList = row.inReviewList, totalCorrect = row.totalCorrect,
                        bestStreak = row.bestStreak, currentStatsStreak = row.currentStatsStreak,
                    ),
                )
            }
        }
        if (sentenceIdMap.isNotEmpty()) {
            val rows = runCatching {
                supabase.from("sentence_progress")
                    .select(
                        Columns.list(
                            "sentence_id", "rating", "direct_streak", "reverse_streak", "audio_streak", "voice_streak",
                            "times_seen", "total_correct", "best_streak", "current_stats_streak", "known",
                        ),
                    ) { filter { eq("user_id", userId); isIn("sentence_id", sentenceIdMap.keys.toList()) } }
                    .decodeList<RemoteSentenceProgressRow>()
            }.getOrDefault(emptyList())
            for (row in rows) {
                val localId = sentenceIdMap[row.sentenceId] ?: continue
                val existing = sentenceDao.getById(localId) ?: continue
                sentenceDao.update(
                    existing.copy(
                        rating = row.rating, directStreak = row.directStreak, reverseStreak = row.reverseStreak,
                        audioStreak = row.audioStreak, voiceStreak = row.voiceStreak, timesSeen = row.timesSeen,
                        totalCorrect = row.totalCorrect, bestStreak = row.bestStreak, currentStatsStreak = row.currentStatsStreak,
                        known = row.known,
                    ),
                )
            }
        }
    }

    /** Best-effort push of one word's current progress to `word_progress` — a no-op for a word
     * that's never been synced from the server ([Word.remoteId] null, i.e. "Власний матеріал"),
     * whose progress stays purely local exactly as before this redesign. Called from
     * [com.lexumi.app.data.repository.WordRepositoryImpl.updateWord] after every local progress
     * write. Swallows failures (offline, etc.) — a study session never blocks or fails on a network
     * hiccup; the next successful [syncTopicContent] pull picks up whatever's on the server anyway. */
    suspend fun pushWordProgress(word: Word) {
        val remoteWordId = word.remoteId ?: return
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        runCatching {
            supabase.from("word_progress").upsert(
                RemoteWordProgressUpsert(
                    userId = userId, wordId = remoteWordId, rating = word.rating, correctStreak = word.correctStreak,
                    typedStreak = word.typedStreak, typedReverseActive = word.typedReverseActive, voiceStreak = word.voiceStreak,
                    finalStreak = word.finalStreak, timesSeen = word.timesSeen, inReviewList = word.inReviewList,
                    totalCorrect = word.totalCorrect, bestStreak = word.bestStreak, currentStatsStreak = word.currentStatsStreak,
                ),
            ) { onConflict = "user_id,word_id" }
        }
    }

    /** Mirrors [pushWordProgress] for sentences. */
    suspend fun pushSentenceProgress(sentence: Sentence) {
        val remoteSentenceId = sentence.remoteId ?: return
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        runCatching {
            supabase.from("sentence_progress").upsert(
                RemoteSentenceProgressUpsert(
                    userId = userId, sentenceId = remoteSentenceId, rating = sentence.rating, directStreak = sentence.directStreak,
                    reverseStreak = sentence.reverseStreak, audioStreak = sentence.audioStreak, voiceStreak = sentence.voiceStreak,
                    timesSeen = sentence.timesSeen, totalCorrect = sentence.totalCorrect, bestStreak = sentence.bestStreak,
                    currentStatsStreak = sentence.currentStatsStreak, known = sentence.known,
                ),
            ) { onConflict = "user_id,sentence_id" }
        }
    }

    /** Deletes a topic's own leaf content (images/stories/videos+questions/audio+questions — none
     * of it shared with any other topic) plus this device's local copy of the words/sentences it
     * referenced, but ONLY the ones no OTHER locally-cached topic still needs (checked via
     * [WordDao.countLinks]/[SentenceDao.countLinks] after this topic's own links are removed). The
     * topic row itself and its progress on the server are untouched — this only clears the local
     * cache; [syncTopicContent] rebuilds content AND pulls progress back the next time the topic is
     * opened. Never touches a topic the user created themselves (`remoteId == null`). */
    private suspend fun evictTopicContent(localTopicId: Long) {
        val topic = topicDao.getById(localTopicId) ?: return
        if (topic.remoteId == null) return

        val wordLinks = wordTopicCrossRefDao.getForTopic(localTopicId)
        val sentenceLinks = sentenceTopicCrossRefDao.getForTopic(localTopicId)
        wordTopicCrossRefDao.deleteAllForTopic(localTopicId)
        sentenceTopicCrossRefDao.deleteAllForTopic(localTopicId)
        for (link in wordLinks) {
            if (wordDao.countLinks(link.wordId) == 0) {
                val word = wordDao.getById(link.wordId)
                if (word?.remoteId != null) wordDao.deleteById(word.id)
            }
        }
        for (link in sentenceLinks) {
            if (sentenceDao.countLinks(link.sentenceId) == 0) {
                val sentence = sentenceDao.getById(link.sentenceId)
                if (sentence?.remoteId != null) sentenceDao.deleteById(sentence.id)
            }
        }

        for (image in imageContentDao.getForTopic(localTopicId)) imageContentDao.delete(image)
        for (story in storyDao.getForTopic(localTopicId)) storyDao.delete(story)
        for (video in videoDao.getForTopic(localTopicId)) {
            testQuestionDao.deleteAllForOwner(QuestionOwnerType.VIDEO, video.id)
            videoDao.delete(video)
        }
        for (dialog in audioDialogDao.getForTopic(localTopicId)) {
            testQuestionDao.deleteAllForOwner(QuestionOwnerType.AUDIO_DIALOG, dialog.id)
            File(dialog.audioPath).delete()
            audioDialogDao.delete(dialog)
        }
    }

    /** Evicts every admin topic's cached content in [languageId] except [exceptTopicId] — at most
     * one topic's content is meant to be cached at a time (see the plan notes: this is deliberate,
     * not just a storage optimization — offline access to "Самостійне вивчення" content is meant to
     * be limited to the one topic currently in progress). */
    suspend fun evictStaleTopicContent(languageId: Long, exceptTopicId: Long?) {
        for (section in sectionDao.getForLanguage(languageId)) {
            for (topic in topicDao.getForSection(section.id)) {
                if (topic.remoteId != null && topic.id != exceptTopicId) evictTopicContent(topic.id)
            }
        }
    }

    /** Same as [evictStaleTopicContent], resolving the language from [exceptTopicId] itself —
     * convenient for call sites (like TopicActionViewModel) that only know the topic being opened. */
    suspend fun evictOtherTopicsContent(exceptTopicId: Long) {
        val topic = topicDao.getById(exceptTopicId) ?: return
        val section = sectionDao.getById(topic.sectionId) ?: return
        evictStaleTopicContent(section.languageId, exceptTopicId)
    }
}
