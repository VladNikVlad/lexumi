package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.SectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {
    @Query("SELECT * FROM sections WHERE languageId = :languageId ORDER BY position ASC, id ASC")
    fun observeForLanguage(languageId: Long): Flow<List<SectionEntity>>

    /** Sections synced from an admin-published language ("Самостійне вивчення") — always
     * read-only on Android now (editing admin content is a web-panel-only job). */
    @Query("SELECT * FROM sections WHERE languageId = :languageId AND remoteId IS NOT NULL ORDER BY position ASC, id ASC")
    fun observeAdminSections(languageId: Long): Flow<List<SectionEntity>>

    /** Sections the user created themselves ("Власний матеріал") — never touched by sync. */
    @Query("SELECT * FROM sections WHERE languageId = :languageId AND remoteId IS NULL ORDER BY position ASC, id ASC")
    fun observePersonalSections(languageId: Long): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE languageId = :languageId ORDER BY position ASC, id ASC")
    suspend fun getForLanguage(languageId: Long): List<SectionEntity>

    @Query("UPDATE sections SET remoteId = :remoteId WHERE id = :id")
    suspend fun setRemoteId(id: Long, remoteId: String)

    @Query("SELECT * FROM sections WHERE id = :id")
    suspend fun getById(id: Long): SectionEntity?

    @Query("SELECT * FROM sections WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): SectionEntity?

    @Query("SELECT COUNT(*) FROM sections WHERE languageId = :languageId AND lower(name) = lower(:name)")
    suspend fun countByName(languageId: Long, name: String): Int

    @Query("SELECT COUNT(*) FROM sections WHERE languageId = :languageId")
    suspend fun countForLanguage(languageId: Long): Int

    @Insert
    suspend fun insert(section: SectionEntity): Long

    @Update
    suspend fun update(section: SectionEntity)

    @Delete
    suspend fun delete(section: SectionEntity)
}
