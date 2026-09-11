package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.ImageContentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageContentDao {
    @Query("SELECT * FROM image_contents WHERE topicId = :topicId")
    fun observeForTopic(topicId: Long): Flow<List<ImageContentEntity>>

    @Query("SELECT * FROM image_contents WHERE topicId = :topicId")
    suspend fun getForTopic(topicId: Long): List<ImageContentEntity>

    @Query("SELECT COUNT(*) FROM image_contents WHERE topicId = :topicId AND lower(name) = lower(:name)")
    suspend fun countByName(topicId: Long, name: String): Int

    @Query("SELECT * FROM image_contents WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): ImageContentEntity?

    @Insert
    suspend fun insert(image: ImageContentEntity): Long

    @Query("UPDATE image_contents SET remoteId = :remoteId WHERE id = :id")
    suspend fun setRemoteId(id: Long, remoteId: String)

    @Update
    suspend fun update(image: ImageContentEntity)

    @Delete
    suspend fun delete(image: ImageContentEntity)
}
