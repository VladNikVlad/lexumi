package com.lexumi.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sentences",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("topicId")],
)
data class SentenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val name: String,
    val text: String,
    // first entry is the primary translation, the rest are additional valid ones
    val translations: List<String> = emptyList(),
    val ruleIds: List<Long> = emptyList(),
    val score: Double = 0.0,
    val timesSeen: Int = 0,
    val totalCorrect: Int = 0,
    val bestStreak: Int = 0,
    val currentStatsStreak: Int = 0,
)
