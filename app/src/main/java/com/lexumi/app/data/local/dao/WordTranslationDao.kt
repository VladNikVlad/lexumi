package com.lexumi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lexumi.app.data.local.entity.WordTranslationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordTranslationDao {
    @Query("SELECT * FROM word_translations WHERE wordId IN (:wordIds) AND locale = :locale")
    fun observeForWords(wordIds: List<Long>, locale: String): Flow<List<WordTranslationEntity>>

    @Query("SELECT * FROM word_translations WHERE wordId IN (:wordIds) AND locale = :locale")
    suspend fun getForWords(wordIds: List<Long>, locale: String): List<WordTranslationEntity>

    @Query("SELECT * FROM word_translations WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): WordTranslationEntity?

    @Insert
    suspend fun insert(translation: WordTranslationEntity): Long

    @Update
    suspend fun update(translation: WordTranslationEntity)
}
