package com.lexumi.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A local learner profile — exactly one per signed-in Supabase/Google account ([authUserId]),
 * created automatically on first sign-in (see SplashViewModel). Content (languages/sections/...)
 * is scoped to this row's id, never to [authUserId] directly, so the rest of the app never needs
 * to know about auth at all. */
@Entity(tableName = "user_profiles", indices = [Index("authUserId")])
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val authUserId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
