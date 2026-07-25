package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.ImageContentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageContentDao {
    @Query("SELECT * FROM image_contents WHERE topicId = :topicId")
    fun observeForTopic(topicId: Long): Flow<List<ImageContentEntity>>

    @Query("SELECT COUNT(*) FROM image_contents WHERE topicId = :topicId AND lower(name) = lower(:name)")
    suspend fun countByName(topicId: Long, name: String): Int

    @Insert
    suspend fun insert(image: ImageContentEntity): Long

    @Update
    suspend fun update(image: ImageContentEntity)

    @Delete
    suspend fun delete(image: ImageContentEntity)
}
