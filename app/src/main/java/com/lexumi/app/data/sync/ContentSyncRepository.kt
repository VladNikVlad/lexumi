package com.lexumi.app.data.sync

import com.lexumi.app.data.auth.AuthRepository
import com.lexumi.app.data.local.dao.LanguageDao
import com.lexumi.app.data.local.dao.SectionDao
import com.lexumi.app.data.local.dao.TopicDao
import com.lexumi.app.data.local.dao.WordDao
import com.lexumi.app.data.local.entity.LanguageEntity
import com.lexumi.app.data.local.entity.SectionEntity
import com.lexumi.app.data.local.entity.TopicEntity
import com.lexumi.app.data.local.entity.WordEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

// --- rows exactly as they exist on the Supabase side (see backend/SCHEMA.md) ---
// Only the columns this pass actually needs are listed — Postgres fills in every
// other column (rating, streaks, timestamps...) from its own defaults on insert.

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
)

/** A language available to download — shown to non-admin users so they can pick admin-authored content. */
data class DownloadableLanguage(val remoteId: String, val name: String, val voiceName: String?)

/**
 * Pushes an admin's local content up to Supabase as global (`owner_id = null`) content, and lets
 * any user pull already-published content down into their own local Room copy. This is
 * intentionally a one-way "publish, then download a copy" flow rather than live two-way sync —
 * each downloaded word/topic/etc. becomes an ordinary local row from then on (its own rating,
 * its own streaks), exactly like anything the user creates themselves. See backend/SCHEMA.md for
 * why per-user progress can't simply live on the shared/global row.
 */
@Singleton
class ContentSyncRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository,
    private val languageDao: LanguageDao,
    private val sectionDao: SectionDao,
    private val topicDao: TopicDao,
    private val wordDao: WordDao,
) {

    /** Pushes this language and everything under it (sections, topics, words) that hasn't
     * already been published. Safe to call again later — anything with a `remoteId` already is
     * left untouched, only newly-added local content gets pushed. Throws if the signed-in user
     * isn't an admin (also enforced server-side by Row Level Security either way). */
    suspend fun publishLanguage(languageId: Long) {
        val profile = authRepository.getMyProfile()
        check(profile?.isAdmin == true) { "Публікувати може лише адмін" }

        val language = languageDao.getById(languageId) ?: return
        val remoteLanguageId = language.remoteId ?: run {
            val inserted = supabase.from("languages")
                .insert(RemoteLanguageRow(ownerId = null, name = language.name, voiceName = language.voiceName)) {
                    select(Columns.list("id"))
                }
                .decodeSingle<RemoteLanguageRow>()
            val id = requireNotNull(inserted.id)
            languageDao.setRemoteId(language.id, id)
            id
        }

        for (section in sectionDao.getForLanguage(languageId)) {
            val remoteSectionId = section.remoteId ?: run {
                val inserted = supabase.from("sections")
                    .insert(RemoteSectionRow(ownerId = null, languageId = remoteLanguageId, name = section.name, position = section.position)) {
                        select(Columns.list("id"))
                    }
                    .decodeSingle<RemoteSectionRow>()
                val id = requireNotNull(inserted.id)
                sectionDao.setRemoteId(section.id, id)
                id
            }

            for (topic in topicDao.getForSection(section.id)) {
                val remoteTopicId = topic.remoteId ?: run {
                    val inserted = supabase.from("topics")
                        .insert(RemoteTopicRow(ownerId = null, sectionId = remoteSectionId, name = topic.name, position = topic.position)) {
                            select(Columns.list("id"))
                        }
                        .decodeSingle<RemoteTopicRow>()
                    val id = requireNotNull(inserted.id)
                    topicDao.setRemoteId(topic.id, id)
                    id
                }

                for (word in wordDao.getForTopic(topic.id)) {
                    if (word.remoteId != null) continue // already published, don't duplicate
                    val inserted = supabase.from("words")
                        .insert(RemoteWordRow(ownerId = null, topicId = remoteTopicId, term = word.term, translation = word.translation)) {
                            select(Columns.list("id"))
                        }
                        .decodeSingle<RemoteWordRow>()
                    wordDao.setRemoteId(word.id, requireNotNull(inserted.id))
                }
            }
        }
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
                    .select(Columns.list("id", "term", "translation")) { filter { eq("topic_id", remoteTopicId) } }
                    .decodeList<RemoteWordRow>()
                for (wordRow in words) {
                    wordDao.insert(
                        WordEntity(topicId = localTopicId, term = wordRow.term, translation = wordRow.translation, remoteId = wordRow.id),
                    )
                }
            }
        }
        return localLanguageId
    }
}
