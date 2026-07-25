package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.SectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {
    @Query("SELECT * FROM sections WHERE languageId = :languageId ORDER BY position ASC, id ASC")
    fun observeForLanguage(languageId: Long): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE id = :id")
    suspend fun getById(id: Long): SectionEntity?

    @Query("SELECT COUNT(*) FROM sections WHERE languageId = :languageId AND lower(name) = lower(:name)")
    suspend fun countByName(languageId: Long, name: String): Int

    @Query("SELECT COUNT(*) FROM sections WHERE languageId = :languageId")
    suspend fun countForLanguage(languageId: Long): Int

    @Insert
    suspend fun insert(section: SectionEntity): Long

    @Delete
    suspend fun delete(section: SectionEntity)
}
