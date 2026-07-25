package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules WHERE languageId = :languageId ORDER BY name ASC")
    fun observeForLanguage(languageId: Long): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<RuleEntity>

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getById(id: Long): RuleEntity?

    @Query("SELECT COUNT(*) FROM rules WHERE languageId = :languageId AND lower(name) = lower(:name)")
    suspend fun countByName(languageId: Long, name: String): Int

    @Insert
    suspend fun insert(rule: RuleEntity): Long

    @Delete
    suspend fun delete(rule: RuleEntity)
}
