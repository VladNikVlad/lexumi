package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.AudioDialogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioDialogDao {
    @Query("SELECT * FROM audio_dialogs WHERE topicId = :topicId")
    fun observeForTopic(topicId: Long): Flow<List<AudioDialogEntity>>

    @Query("SELECT * FROM audio_dialogs WHERE topicId = :topicId")
    suspend fun getForTopic(topicId: Long): List<AudioDialogEntity>

    @Query("SELECT * FROM audio_dialogs WHERE id = :id")
    suspend fun getById(id: Long): AudioDialogEntity?

    @Query("SELECT * FROM audio_dialogs WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): AudioDialogEntity?

    @Query("SELECT COUNT(*) FROM audio_dialogs WHERE topicId = :topicId AND lower(name) = lower(:name)")
    suspend fun countByName(topicId: Long, name: String): Int

    @Insert
    suspend fun insert(dialog: AudioDialogEntity): Long

    @Update
    suspend fun update(dialog: AudioDialogEntity)

    @Query("UPDATE audio_dialogs SET remoteId = :remoteId WHERE id = :id")
    suspend fun setRemoteId(id: Long, remoteId: String)

    @Delete
    suspend fun delete(dialog: AudioDialogEntity)
}
