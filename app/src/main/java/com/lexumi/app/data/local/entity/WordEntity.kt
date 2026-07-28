package com.lexumi.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
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
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val imagePath: String? = null,
    val term: String,
    val translation: String,
    val ruleId: Long? = null,
    // --- spaced-repetition state (hidden from the user, point 20 of the scenario) ---
    val level: Int = 0,               // 0 = multiple choice, 1 = free text input
    val correctStreak: Int = 0,       // consecutive correct answers at level 0, needs 5 to reach level 1
    val score: Double = 0.0,          // accumulates at level 1, 1.0 correct / 0.5 one-letter typo, reaches 10 to "master"
    val timesSeen: Int = 0,
    val lastSeenAt: Long? = null,
    val inReviewList: Boolean = false,
    val addedToReviewAt: Long? = null,
    // --- lifetime stats shown to the user (point 5): how many times
    // reviewed, how many exactly right, and the longest correct-in-a-row streak ever ---
    val totalCorrect: Int = 0,
    val bestStreak: Int = 0,
    val currentStatsStreak: Int = 0,
)
