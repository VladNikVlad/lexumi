package com.lexumi.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Translation of a [WordEntity] into a locale OTHER than the default ('uk', which lives directly
 * on [WordEntity.translations]) — see backend/word_sentence_locale_translations.sql. A missing row
 * for a given (wordId, locale) just means "not yet translated into that locale"; callers fall back
 * to the word's own base translation rather than showing blank (see RepositoryImpl.kt's resolution).
 */
@Entity(
    tableName = "word_translations",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("wordId"), Index(value = ["wordId", "locale"], unique = true)],
)
data class WordTranslationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordId: Long,
    val locale: String,
    val translations: List<String> = emptyList(),
    val remoteId: String? = null, // Supabase uuid, once synced from the admin panel
)
