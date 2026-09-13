package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.LanguageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LanguageDao {
    /** Only [profileId]'s own languages — each signed-in account has its own (see
     * ProfileRepository/SplashViewModel for how profileId maps to an account). */
    @Query("SELECT * FROM languages WHERE profileId = :profileId ORDER BY createdAt ASC")
    fun observeForProfile(profileId: Long): Flow<List<LanguageEntity>>

    @Query("SELECT * FROM languages WHERE id = :id")
    suspend fun getById(id: Long): LanguageEntity?

    @Query("SELECT COUNT(*) FROM languages WHERE profileId = :profileId AND lower(name) = lower(:name)")
    suspend fun countByName(profileId: Long, name: String): Int

    @Insert
    suspend fun insert(language: LanguageEntity): Long

    @Query("UPDATE languages SET voiceName = :voiceName WHERE id = :languageId")
    suspend fun setVoice(languageId: Long, voiceName: String?)

    @Query("UPDATE languages SET remoteId = :remoteId WHERE id = :id")
    suspend fun setRemoteId(id: Long, remoteId: String)

    @Query("SELECT * FROM languages WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): LanguageEntity?

    /** Whether [profileId] specifically already has its own local copy of this admin-published
     * language — used to decide what to still offer in "Доступно для завантаження" (a language
     * another account downloaded shouldn't disappear from THIS account's list). */
    @Query("SELECT * FROM languages WHERE remoteId = :remoteId AND profileId = :profileId LIMIT 1")
    suspend fun getByRemoteIdForProfile(remoteId: String, profileId: Long): LanguageEntity?

    @Update
    suspend fun update(language: LanguageEntity)

    @Delete
    suspend fun delete(language: LanguageEntity)
}
