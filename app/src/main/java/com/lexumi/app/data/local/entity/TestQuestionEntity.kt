package com.lexumi.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class QuestionOwnerType { VIDEO, AUDIO_DIALOG }
enum class AnswerType { TRUE_FALSE, EXACT_TEXT }

/**
 * Optional control questions attached to a video or audio dialogue (point 9 & 10).
 * Two answer modes: a true/false checkbox, or an exact-text answer that accepts
 * several equally valid answers.
 */
@Entity(tableName = "test_questions")
data class TestQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerType: QuestionOwnerType,
    val ownerId: Long,
    val questionText: String,
    val answerType: AnswerType,
    val correctBoolean: Boolean? = null,          // used when answerType == TRUE_FALSE
    val acceptableAnswers: List<String> = emptyList(), // used when answerType == EXACT_TEXT
)
