package com.lexumi.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Rules are unique within a language as a whole (not per section/topic),
 * per point 7 of the scenario, so they can be re-used and attached to
 * words, sentences, video, audio and stories across the whole language.
 */
@Entity(
    tableName = "rules",
    foreignKeys = [
        ForeignKey(
            entity = LanguageEntity::class,
            parentColumns = ["id"],
            childColumns = ["languageId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("languageId")],
)
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val languageId: Long,
    val name: String,
    val text: String,
)
