package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.SentenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SentenceDao {
    @Query("SELECT * FROM sentences WHERE languageId = :languageId")
    suspend fun getForLanguage(languageId: Long): List<SentenceEntity>

    @Query("SELECT * FROM sentences WHERE id IN (:ids)")
    fun observeByIds(ids: List<Long>): Flow<List<SentenceEntity>>

    @Query("SELECT * FROM sentences WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<SentenceEntity>

    @Query("SELECT * FROM sentences WHERE id = :id")
    suspend fun getById(id: Long): SentenceEntity?

    @Query("SELECT * FROM sentences WHERE languageId = :languageId AND lower(trim(text)) = lower(trim(:text)) LIMIT 1")
    suspend fun findByLanguageAndText(languageId: Long, text: String): SentenceEntity?

    @Query("SELECT COUNT(*) FROM sentence_topic_cross_ref WHERE sentenceId = :sentenceId")
    suspend fun countLinks(sentenceId: Long): Int

    @Insert
    suspend fun insert(sentence: SentenceEntity): Long

    @Query("UPDATE sentences SET remoteId = :remoteId WHERE id = :id")
    suspend fun setRemoteId(id: Long, remoteId: String)

    @Update
    suspend fun update(sentence: SentenceEntity)

    @Query("DELETE FROM sentences WHERE id = :id")
    suspend fun deleteById(id: Long)
}
