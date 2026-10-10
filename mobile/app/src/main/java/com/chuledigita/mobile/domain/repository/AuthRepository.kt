package com.chuledigita.mobile.domain.repository

import com.chuledigita.mobile.domain.model.UserSession
import com.chuledigita.mobile.domain.model.FamilyMember
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
     * Registers a new family member.
     */
    suspend fun register(
        fullName: String,
        phone: String?,
        username: String,
        password: String
    ): Result<FamilyMember>

    /**
     * Flow emitting the current active session.
     */
    val currentSessionFlow: Flow<UserSession?>

    /**
     * Ends the current session and clears local credentials.
     */
    suspend fun logout()
}
