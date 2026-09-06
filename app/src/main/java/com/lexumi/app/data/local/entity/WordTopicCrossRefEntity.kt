package com.lexumi.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Links a shared [WordEntity] to a topic that uses it. A word can be linked from many topics
 * (of the same language) at once — this is what lets "additional-verbs" and "conjugation-basics"
 * both use the same "hablar" word row instead of duplicating it.
 */
@Entity(
    tableName = "word_topic_cross_ref",
    foreignKeys = [
        ForeignKey(entity = TopicEntity::class, parentColumns = ["id"], childColumns = ["topicId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = WordEntity::class, parentColumns = ["id"], childColumns = ["wordId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("topicId"), Index("wordId")],
)
data class WordTopicCrossRefEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val wordId: Long,
    // null = this topic shows the word's own shared default translation (translations[0]);
    // non-null = this topic forked its own translation, independent of the shared word.
    val translationOverride: String? = null,
    val position: Int = 0,
    val remoteId: String? = null, // Supabase uuid of the topic_words row, once published
)
