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

    @Query("SELECT COUNT(*) FROM sentences WHERE topicId = :topicId AND lower(name) = lower(:name)")
    suspend fun countByName(topicId: Long, name: String): Int

    @Insert
    suspend fun insert(sentence: SentenceEntity): Long

    @Update
    suspend fun update(sentence: SentenceEntity)

    @Delete
    suspend fun delete(sentence: SentenceEntity)
}
