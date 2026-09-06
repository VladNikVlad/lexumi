package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE id = :id")
    suspend fun getById(id: Long): UserProfileEntity?

    @Query("SELECT displayName FROM user_profiles")
    suspend fun getAllDisplayNames(): List<String>

    @Insert
    suspend fun insert(profile: UserProfileEntity): Long

    @Query("UPDATE user_profiles SET displayName = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Delete
    suspend fun delete(profile: UserProfileEntity)

    @Query("SELECT COUNT(*) FROM user_profiles")
    suspend fun count(): Int
}
