package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos WHERE topicId = :topicId")
    fun observeForTopic(topicId: Long): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getById(id: Long): VideoEntity?

    @Query("SELECT COUNT(*) FROM videos WHERE topicId = :topicId AND lower(name) = lower(:name)")
    suspend fun countByName(topicId: Long, name: String): Int

    @Insert
    suspend fun insert(video: VideoEntity): Long

    @Delete
    suspend fun delete(video: VideoEntity)
}
