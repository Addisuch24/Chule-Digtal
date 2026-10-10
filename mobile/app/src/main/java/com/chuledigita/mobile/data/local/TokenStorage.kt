package com.chuledigita.mobile.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chuledigita.mobile.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chule_user_session")

/**
 * Manages secure local persistence of JWT authentication token and user session details
 * using Jetpack DataStore Preferences.
 */
class TokenStorage(private val context: Context) {

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_USER_ID = longPreferencesKey("user_id")
        private val KEY_FULL_NAME = stringPreferencesKey("user_full_name")
        private val KEY_PHONE = stringPreferencesKey("user_phone")
        private val KEY_USERNAME = stringPreferencesKey("user_username")
        private val KEY_IS_ACTIVE = booleanPreferencesKey("user_is_active")
    }

    /**
     * Flow emitting the current JWT authentication token, or null if not logged in.
     */
    val tokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_TOKEN]
    }

    /**
     * Flow emitting the active [UserSession], or null if no session exists.
     */
    val userSessionFlow: Flow<UserSession?> = context.dataStore.data.map { preferences ->
        val token = preferences[KEY_TOKEN] ?: return@map null
        val id = preferences[KEY_USER_ID] ?: return@map null
        val fullName = preferences[KEY_FULL_NAME] ?: ""
        val phone = preferences[KEY_PHONE] ?: ""
        val username = preferences[KEY_USERNAME] ?: ""
        val isActive = preferences[KEY_IS_ACTIVE] ?: true

        UserSession(
            id = id,
            fullName = fullName,
            phone = phone,
            username = username,
            isActive = isActive,
            token = token
        )
    }

    /**
     * Saves JWT token and user session details into DataStore.
     */
    suspend fun saveSession(session: UserSession) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TOKEN] = session.token
            preferences[KEY_USER_ID] = session.id
            preferences[KEY_FULL_NAME] = session.fullName
            preferences[KEY_PHONE] = session.phone
            preferences[KEY_USERNAME] = session.username
            preferences[KEY_IS_ACTIVE] = session.isActive
        }
    }

    /**
     * Updates only the JWT auth token.
     */
    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TOKEN] = token
        }
    }

    /**
     * Clears all session preferences upon logout.
     */
    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
