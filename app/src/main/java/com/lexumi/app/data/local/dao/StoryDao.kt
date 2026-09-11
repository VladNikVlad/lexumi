package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.StoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories WHERE topicId = :topicId")
    fun observeForTopic(topicId: Long): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE topicId = :topicId")
    suspend fun getForTopic(topicId: Long): List<StoryEntity>

    @Query("SELECT * FROM stories WHERE id = :id")
    suspend fun getById(id: Long): StoryEntity?

    @Query("SELECT * FROM stories WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): StoryEntity?

    @Query("SELECT COUNT(*) FROM stories WHERE topicId = :topicId AND lower(name) = lower(:name)")
    suspend fun countByName(topicId: Long, name: String): Int

    @Insert
    suspend fun insert(story: StoryEntity): Long

    @Update
    suspend fun update(story: StoryEntity)

    @Query("UPDATE stories SET remoteId = :remoteId WHERE id = :id")
    suspend fun setRemoteId(id: Long, remoteId: String)

    @Delete
    suspend fun delete(story: StoryEntity)
}
