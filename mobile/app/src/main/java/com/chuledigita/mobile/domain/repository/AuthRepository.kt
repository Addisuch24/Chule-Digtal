package com.chuledigita.mobile.domain.repository

import com.chuledigita.mobile.domain.model.FamilyMember
import com.chuledigita.mobile.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining authentication and family member identification contracts.
 * Decouples domain logic from Retrofit API network calls and DataStore local persistence.
 */
interface AuthRepository {
    /**
     * Authenticates a user and starts a session.
     */
    suspend fun login(username: String, password: String): Result<UserSession>

    /**
     * Registers a new family member using the 5 required fields.
     */
    suspend fun register(
        fullName: String,
        phone: String?,
        email: String,
        username: String,
        password: String
    ): Result<FamilyMember>

    /**
     * Fetches current authenticated user's profile from /api/auth/me.
     */
    suspend fun getProfile(): Result<UserSession>

    /**
     * Flow emitting the current active session.
     */
    val currentSessionFlow: Flow<UserSession?>

    /**
     * Ends the current session and clears local credentials.
     */
    suspend fun logout()
}
