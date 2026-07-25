package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.LanguageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LanguageDao {
    @Query("SELECT * FROM languages WHERE profileId = :profileId ORDER BY createdAt ASC")
    fun observeAll(profileId: Long): Flow<List<LanguageEntity>>

    @Query("SELECT * FROM languages WHERE id = :id")
    suspend fun getById(id: Long): LanguageEntity?

    @Query("SELECT COUNT(*) FROM languages WHERE profileId = :profileId AND lower(name) = lower(:name)")
    suspend fun countByName(profileId: Long, name: String): Int

    @Insert
    suspend fun insert(language: LanguageEntity): Long

    @Query("UPDATE languages SET voiceName = :voiceName WHERE id = :languageId")
    suspend fun setVoice(languageId: Long, voiceName: String?)

    @Update
    suspend fun update(language: LanguageEntity)

    @Delete
    suspend fun delete(language: LanguageEntity)
}
