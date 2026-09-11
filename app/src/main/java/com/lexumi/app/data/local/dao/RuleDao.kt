package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules WHERE languageId = :languageId ORDER BY name ASC")
    fun observeForLanguage(languageId: Long): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE languageId = :languageId ORDER BY name ASC")
    suspend fun getForLanguage(languageId: Long): List<RuleEntity>

    @Query("SELECT * FROM rules WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<RuleEntity>

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getById(id: Long): RuleEntity?

    @Query("SELECT * FROM rules WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): RuleEntity?

    @Query("SELECT COUNT(*) FROM rules WHERE languageId = :languageId AND lower(name) = lower(:name)")
    suspend fun countByName(languageId: Long, name: String): Int

    @Insert
    suspend fun insert(rule: RuleEntity): Long

    @Update
    suspend fun update(rule: RuleEntity)

    @Query("UPDATE rules SET remoteId = :remoteId WHERE id = :id")
    suspend fun setRemoteId(id: Long, remoteId: String)

    @Delete
    suspend fun delete(rule: RuleEntity)
}
