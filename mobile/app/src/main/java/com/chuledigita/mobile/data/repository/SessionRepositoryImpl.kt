package com.chuledigita.mobile.data.repository

import com.chuledigita.mobile.data.local.TokenStorage
import com.chuledigita.mobile.domain.model.UserSession
import com.chuledigita.mobile.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking

/**
 * Implementation of [SessionRepository] wrapping [TokenStorage] DataStore manager.
 * Includes an in-memory cache of the token for synchronous access by interceptors.
 */
class SessionRepositoryImpl(
    private val tokenStorage: TokenStorage
) : SessionRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    @Volatile
    private var inMemoryToken: String? = null

    // Maintain an in-memory StateFlow for synchronous access without runBlocking in interceptors
    private val tokenState: StateFlow<String?> = tokenStorage.tokenFlow
        .onEach { inMemoryToken = it }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    override val tokenFlow: Flow<String?> = tokenStorage.tokenFlow

    override val userSessionFlow: Flow<UserSession?> = tokenStorage.userSessionFlow

    override fun getCachedToken(): String? {
        val current = inMemoryToken ?: tokenState.value
        if (current != null) return current
        return runCatching {
            runBlocking(Dispatchers.IO) {
                tokenStorage.tokenFlow.firstOrNull()
            }
        }.getOrNull().also { inMemoryToken = it }
    }

    override suspend fun saveSession(session: UserSession) {
        inMemoryToken = session.token
        tokenStorage.saveSession(session)
    }

    override suspend fun saveToken(token: String) {
        inMemoryToken = token
        tokenStorage.saveToken(token)
    }

    override suspend fun clearSession() {
        inMemoryToken = null
        tokenStorage.clearSession()
    }

    override fun clearSessionSync() {
        inMemoryToken = null
        runCatching {
            runBlocking(Dispatchers.IO) {
                tokenStorage.clearSession()
            }
        }
    }
}
