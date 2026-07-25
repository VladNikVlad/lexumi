package com.lexumi.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "languages")
data class LanguageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val voiceName: String? = null,
)
