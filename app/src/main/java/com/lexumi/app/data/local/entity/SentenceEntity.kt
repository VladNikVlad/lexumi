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
    // --- mastery ladder (rating 0-4), mirrors words ---
    // 0 = target->native typed, 1 = native->target typed, 2 = audio-only (hear target, type native),
    // 3 = say-it-aloud (native shown, speak target), 4 = mastered, excluded from practice.
    val rating: Int = 0,
    val directStreak: Int = 0,   // rating 0: consecutive correct answers, target text -> typed native
    val reverseStreak: Int = 0,  // rating 1: consecutive correct answers, native text -> typed target
    val audioStreak: Int = 0,    // rating 2: consecutive correct answers, heard target -> typed native
    val voiceStreak: Int = 0,    // rating 3: consecutive correct answers, native text -> spoken target
    val score: Double = 0.0,     // legacy, kept for old rows; superseded by `rating`
    val timesSeen: Int = 0,
    val totalCorrect: Int = 0,
    val bestStreak: Int = 0,
    val currentStatsStreak: Int = 0,
    val known: Boolean = false,
)
