package com.lexumi.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A local learner profile. Lets one device host several people ("Змінити користувача"). */
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val createdAt: Long = System.currentTimeMillis(),
)
