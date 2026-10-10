package com.chuledigita.mobile.domain.repository

import com.chuledigita.mobile.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for managing local user session lifecycle and token persistence.
 */
interface SessionRepository {
    val tokenFlow: Flow<String?>
    val userSessionFlow: Flow<UserSession?>
    
    /**
     * Synchronously returns the current token if available in memory.
     */
    fun getCachedToken(): String?

    suspend fun saveSession(session: UserSession)
    suspend fun saveToken(token: String)
    suspend fun clearSession()
}
