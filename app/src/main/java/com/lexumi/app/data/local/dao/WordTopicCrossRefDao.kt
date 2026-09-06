package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.WordTopicCrossRefEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordTopicCrossRefDao {
    @Query("SELECT * FROM word_topic_cross_ref WHERE topicId = :topicId ORDER BY position ASC")
    fun observeForTopic(topicId: Long): Flow<List<WordTopicCrossRefEntity>>

    @Query("SELECT * FROM word_topic_cross_ref WHERE topicId = :topicId ORDER BY position ASC")
    suspend fun getForTopic(topicId: Long): List<WordTopicCrossRefEntity>

    @Query("SELECT * FROM word_topic_cross_ref WHERE topicId = :topicId AND wordId = :wordId LIMIT 1")
    suspend fun getLink(topicId: Long, wordId: Long): WordTopicCrossRefEntity?

    @Query("SELECT COUNT(*) FROM word_topic_cross_ref WHERE topicId = :topicId")
    suspend fun countForTopic(topicId: Long): Int

    @Insert
    suspend fun insert(crossRef: WordTopicCrossRefEntity): Long

    @Update
    suspend fun update(crossRef: WordTopicCrossRefEntity)

    @Query("DELETE FROM word_topic_cross_ref WHERE topicId = :topicId AND wordId = :wordId")
    suspend fun deleteLink(topicId: Long, wordId: Long)

    @Query("UPDATE word_topic_cross_ref SET remoteId = :remoteId WHERE id = :id")
    suspend fun setRemoteId(id: Long, remoteId: String)
}
