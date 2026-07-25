package com.lexumi.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "lexumi_prefs")

data class LastSession(val topicId: Long, val screenRoute: String)

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val ds = context.dataStore

    private object Keys {
        val CURRENT_PROFILE_ID = longPreferencesKey("current_profile_id")
        val SELECTED_LANGUAGE_ID = longPreferencesKey("selected_language_id")
        val WORDS_PER_SESSION = intPreferencesKey("words_per_session")
        val REPETITIONS = intPreferencesKey("repetitions")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val LAST_TOPIC_ID = longPreferencesKey("last_topic_id")
        val LAST_ROUTE = stringPreferencesKey("last_route")
    }

    val currentProfileId: Flow<Long?> = ds.data.map { it[Keys.CURRENT_PROFILE_ID] }
    val selectedLanguageId: Flow<Long?> = ds.data.map { it[Keys.SELECTED_LANGUAGE_ID] }
    val wordsPerSession: Flow<Int> = ds.data.map { it[Keys.WORDS_PER_SESSION] ?: 10 }
    val repetitions: Flow<Int> = ds.data.map { it[Keys.REPETITIONS] ?: 2 }
    val remindersEnabled: Flow<Boolean> = ds.data.map { it[Keys.REMINDERS_ENABLED] ?: true }
    val lastSession: Flow<LastSession?> = ds.data.map {
        val topicId = it[Keys.LAST_TOPIC_ID]
        val route = it[Keys.LAST_ROUTE]
        if (topicId != null && route != null) LastSession(topicId, route) else null
    }

    suspend fun setCurrentProfile(id: Long) = ds.edit { it[Keys.CURRENT_PROFILE_ID] = id }

    suspend fun clearCurrentProfile() = ds.edit {
        it.remove(Keys.CURRENT_PROFILE_ID)
        it.remove(Keys.SELECTED_LANGUAGE_ID)
        it.remove(Keys.LAST_TOPIC_ID)
        it.remove(Keys.LAST_ROUTE)
    }

    suspend fun setSelectedLanguage(id: Long) = ds.edit { it[Keys.SELECTED_LANGUAGE_ID] = id }

    suspend fun clearSelectedLanguage() = ds.edit { it.remove(Keys.SELECTED_LANGUAGE_ID) }

    suspend fun setWordsPerSession(count: Int) = ds.edit { it[Keys.WORDS_PER_SESSION] = count }

    suspend fun setRepetitions(count: Int) = ds.edit { it[Keys.REPETITIONS] = count }

    suspend fun setRemindersEnabled(enabled: Boolean) = ds.edit { it[Keys.REMINDERS_ENABLED] = enabled }

    suspend fun setLastSession(topicId: Long, route: String) = ds.edit {
        it[Keys.LAST_TOPIC_ID] = topicId
        it[Keys.LAST_ROUTE] = route
    }

    suspend fun clearLastSession() = ds.edit {
        it.remove(Keys.LAST_TOPIC_ID)
        it.remove(Keys.LAST_ROUTE)
    }
}
