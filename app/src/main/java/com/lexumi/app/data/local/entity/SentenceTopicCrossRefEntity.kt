package com.lexumi.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Links a shared [SentenceEntity] to a topic that uses it — mirrors [WordTopicCrossRefEntity]. */
@Entity(
    tableName = "sentence_topic_cross_ref",
    foreignKeys = [
        ForeignKey(entity = TopicEntity::class, parentColumns = ["id"], childColumns = ["topicId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SentenceEntity::class, parentColumns = ["id"], childColumns = ["sentenceId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("topicId"), Index("sentenceId")],
)
data class SentenceTopicCrossRefEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val sentenceId: Long,
    // null = this topic shows the sentence's own shared translations list;
    // non-null = this topic forked its own translations, independent of the shared sentence.
    val translationsOverride: List<String>? = null,
    val position: Int = 0,
    val remoteId: String? = null, // Supabase uuid of the topic_sentences row, once published
)
