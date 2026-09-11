package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.SentenceTopicCrossRefEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SentenceTopicCrossRefDao {
    @Query("SELECT * FROM sentence_topic_cross_ref WHERE topicId = :topicId ORDER BY position ASC")
    fun observeForTopic(topicId: Long): Flow<List<SentenceTopicCrossRefEntity>>

    @Query("SELECT * FROM sentence_topic_cross_ref WHERE topicId = :topicId ORDER BY position ASC")
    suspend fun getForTopic(topicId: Long): List<SentenceTopicCrossRefEntity>

    @Query("SELECT * FROM sentence_topic_cross_ref WHERE topicId = :topicId AND sentenceId = :sentenceId LIMIT 1")
    suspend fun getLink(topicId: Long, sentenceId: Long): SentenceTopicCrossRefEntity?

    @Query("SELECT COUNT(*) FROM sentence_topic_cross_ref WHERE topicId = :topicId")
    suspend fun countForTopic(topicId: Long): Int

    @Insert
    suspend fun insert(crossRef: SentenceTopicCrossRefEntity): Long

    @Update
    suspend fun update(crossRef: SentenceTopicCrossRefEntity)

    @Query("DELETE FROM sentence_topic_cross_ref WHERE topicId = :topicId AND sentenceId = :sentenceId")
    suspend fun deleteLink(topicId: Long, sentenceId: Long)

    @Query("DELETE FROM sentence_topic_cross_ref WHERE topicId = :topicId")
    suspend fun deleteAllForTopic(topicId: Long)

    @Query("UPDATE sentence_topic_cross_ref SET remoteId = :remoteId WHERE id = :id")
    suspend fun setRemoteId(id: Long, remoteId: String)
}
