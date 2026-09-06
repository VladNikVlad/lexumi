package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.SentenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SentenceDao {
    @Query("SELECT * FROM sentences WHERE topicId = :topicId")
    fun observeForTopic(topicId: Long): Flow<List<SentenceEntity>>

    @Query("SELECT * FROM sentences WHERE topicId = :topicId")
    suspend fun getForTopic(topicId: Long): List<SentenceEntity>

    @Query("SELECT COUNT(*) FROM sentences WHERE topicId = :topicId AND lower(text) = lower(:text)")
    suspend fun countByText(topicId: Long, text: String): Int

    @Insert
    suspend fun insert(sentence: SentenceEntity): Long

    @Query("UPDATE sentences SET remoteId = :remoteId WHERE id = :id")
    suspend fun setRemoteId(id: Long, remoteId: String)

    @Update
    suspend fun update(sentence: SentenceEntity)

    @Delete
    suspend fun delete(sentence: SentenceEntity)
}
