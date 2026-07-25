package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.StoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories WHERE topicId = :topicId")
    fun observeForTopic(topicId: Long): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE id = :id")
    suspend fun getById(id: Long): StoryEntity?

    @Query("SELECT COUNT(*) FROM stories WHERE topicId = :topicId AND lower(name) = lower(:name)")
    suspend fun countByName(topicId: Long, name: String): Int

    @Insert
    suspend fun insert(story: StoryEntity): Long

    @Delete
    suspend fun delete(story: StoryEntity)
}
