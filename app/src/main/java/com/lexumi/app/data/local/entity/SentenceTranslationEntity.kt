package com.lexumi.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Mirrors [WordTranslationEntity] for [SentenceEntity]. */
@Entity(
    tableName = "sentence_translations",
    foreignKeys = [
        ForeignKey(
            entity = SentenceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sentenceId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("sentenceId"), Index(value = ["sentenceId", "locale"], unique = true)],
)
data class SentenceTranslationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sentenceId: Long,
    val locale: String,
    val translations: List<String> = emptyList(),
    val remoteId: String? = null,
)
