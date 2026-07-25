package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.WordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE topicId = :topicId")
    fun observeForTopic(topicId: Long): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE topicId = :topicId")
    suspend fun getForTopic(topicId: Long): List<WordEntity>

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getById(id: Long): WordEntity?

    @Query("SELECT COUNT(*) FROM words WHERE topicId = :topicId AND lower(term) = lower(:term)")
    suspend fun countByTerm(topicId: Long, term: String): Int

    @Query("SELECT * FROM words WHERE inReviewList = 1 ORDER BY addedToReviewAt ASC")
    fun observeReviewList(): Flow<List<WordEntity>>

    @Insert
    suspend fun insert(word: WordEntity): Long

    @Update
    suspend fun update(word: WordEntity)

    @Delete
    suspend fun delete(word: WordEntity)
}
