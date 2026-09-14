package com.lexumi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lexumi.app.data.local.entity.SentenceTranslationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SentenceTranslationDao {
    @Query("SELECT * FROM sentence_translations WHERE sentenceId IN (:sentenceIds) AND locale = :locale")
    fun observeForSentences(sentenceIds: List<Long>, locale: String): Flow<List<SentenceTranslationEntity>>

    @Query("SELECT * FROM sentence_translations WHERE sentenceId IN (:sentenceIds) AND locale = :locale")
    suspend fun getForSentences(sentenceIds: List<Long>, locale: String): List<SentenceTranslationEntity>

    @Query("SELECT * FROM sentence_translations WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): SentenceTranslationEntity?

    @Insert
    suspend fun insert(translation: SentenceTranslationEntity): Long

    @Update
    suspend fun update(translation: SentenceTranslationEntity)
}
