package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.WordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE languageId = :languageId")
    suspend fun getForLanguage(languageId: Long): List<WordEntity>

    @Query("SELECT * FROM words WHERE id IN (:ids)")
    fun observeByIds(ids: List<Long>): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<WordEntity>

    @Query("UPDATE words SET remoteId = :remoteId WHERE id = :id")
    suspend fun setRemoteId(id: Long, remoteId: String)

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getById(id: Long): WordEntity?

    @Query("SELECT * FROM words WHERE languageId = :languageId AND lower(trim(term)) = lower(trim(:term)) LIMIT 1")
    suspend fun findByLanguageAndTerm(languageId: Long, term: String): WordEntity?

    @Query("SELECT * FROM words WHERE inReviewList = 1 ORDER BY addedToReviewAt ASC")
    fun observeReviewList(): Flow<List<WordEntity>>

    @Query("SELECT COUNT(*) FROM word_topic_cross_ref WHERE wordId = :wordId")
    suspend fun countLinks(wordId: Long): Int

    @Insert
    suspend fun insert(word: WordEntity): Long

    @Update
    suspend fun update(word: WordEntity)

    @Query("DELETE FROM words WHERE id = :id")
    suspend fun deleteById(id: Long)
}
