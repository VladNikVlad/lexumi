package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics WHERE sectionId = :sectionId ORDER BY position ASC, id ASC")
    fun observeForSection(sectionId: Long): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE id = :id")
    suspend fun getById(id: Long): TopicEntity?

    @Query("SELECT COUNT(*) FROM topics WHERE sectionId = :sectionId AND lower(name) = lower(:name)")
    suspend fun countByName(sectionId: Long, name: String): Int

    @Insert
    suspend fun insert(topic: TopicEntity): Long

    @Query("UPDATE topics SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    @Delete
    suspend fun delete(topic: TopicEntity)
}
