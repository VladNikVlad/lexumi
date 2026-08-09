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
    // --- mastery ladder (rating 0-4), point 22 of the scenario ---
    // 0 = new (multiple choice), 1 = typed both directions, 2 = say-it-aloud cards,
    // 3 = hear-only (typed or spoken answer), 4 = mastered, excluded from practice.
    val rating: Int = 0,
    val level: Int = 0,               // legacy, kept for old rows; superseded by `rating`
    val correctStreak: Int = 0,       // rating 0: consecutive correct multiple-choice answers, 5 to advance
    val typedStreak: Int = 0,         // rating 1: consecutive correct typed answers in the current direction
    val typedReverseActive: Boolean = false, // rating 1: false = target->native phase, true = native->target phase
    val voiceStreak: Int = 0,         // rating 2: consecutive correct spoken answers in the cards round
    val finalStreak: Int = 0,         // rating 3: consecutive correct hear-only answers
    val score: Double = 0.0,          // legacy, kept for old rows; superseded by `rating`
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
