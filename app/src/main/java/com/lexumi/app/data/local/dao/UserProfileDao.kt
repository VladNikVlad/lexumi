package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = :id")
    suspend fun getById(id: Long): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE id = :id")
    fun observeById(id: Long): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE authUserId = :authUserId LIMIT 1")
    suspend fun getByAuthUserId(authUserId: String): UserProfileEntity?

    @Insert
    suspend fun insert(profile: UserProfileEntity): Long

    @Query("UPDATE user_profiles SET displayName = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)
}
