package com.chuledigita.mobile.auth

import com.chuledigita.mobile.data.remote.ApiService
import com.chuledigita.mobile.data.remote.AuthInterceptor
import com.chuledigita.mobile.data.remote.model.*
import com.chuledigita.mobile.data.repository.AuthRepositoryImpl
import com.chuledigita.mobile.domain.model.UserSession
import com.chuledigita.mobile.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AuthIntegrationTest {

    private class FakeSessionRepository : SessionRepository {
        var token: String? = null
        var session: UserSession? = null

        override val tokenFlow: Flow<String?> get() = flowOf(token)
        override val userSessionFlow: Flow<UserSession?> get() = flowOf(session)

        override fun getCachedToken(): String? = token

        override suspend fun saveSession(session: UserSession) {
            this.session = session
            this.token = session.token
        }

        override suspend fun saveToken(token: String) {
            this.token = token
        }

        override suspend fun clearSession() {
            this.token = null
            this.session = null
        }

        override fun clearSessionSync() {
            this.token = null
            this.session = null
        }
    }

    private lateinit var fakeSessionRepository: FakeSessionRepository
    private lateinit var apiService: ApiService
    private lateinit var authRepository: AuthRepositoryImpl

    private val localApiBaseUrl = "http://127.0.0.1:5000/api/"

    @Before
    fun setUp() {
        com.chuledigita.mobile.data.remote.ApiConfig.BASE_URL = localApiBaseUrl
        fakeSessionRepository = FakeSessionRepository()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(fakeSessionRepository))
            .build()

        apiService = Retrofit.Builder()
            .baseUrl(localApiBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        authRepository = AuthRepositoryImpl(
            apiService = apiService,
            sessionRepository = fakeSessionRepository
        )
    }

    @Test
    fun testHealthEndpoint_returns200() = runBlocking {
        val response = apiService.getHealthStatus()
        assertTrue("Backend health check must be successful", response.isSuccessful)
        assertEquals(200, response.code())
        val body = response.body()
        assertNotNull(body)
        assertTrue(body!!.success)
    }

    @Test
    fun testLogin_invalidCredentials_returnsFailure() = runBlocking {
        val result = authRepository.login("non_existent_user_999", "wrong_password")
        assertTrue("Invalid login must fail", result.isFailure)
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(
            "Error message should mention invalid credentials: $message",
            message.contains("Invalid username or password", ignoreCase = true)
        )
        assertNull("No session should be saved on failed login", fakeSessionRepository.getCachedToken())
    }

    @Test
    fun testLogin_missingUsernameOrPassword_returns400ValidationError() = runBlocking {
        val missingUserRes = apiService.login(LoginRequest("", "pass1234"))
        assertEquals("Missing username must return 400", 400, missingUserRes.code())

        val missingPassRes = apiService.login(LoginRequest("someuser", ""))
        assertEquals("Missing password must return 400", 400, missingPassRes.code())
    }

    @Test
    fun testRegister_duplicateUsername_returnsFailure() = runBlocking {
        // "chule" is the existing active administrator user in the database
        val result = authRepository.register(
            fullName = "Duplicate Chule",
            phone = null,
            email = "chule_dup@gmail.com",
            username = "chule",
            password = "securePassword123"
        )
        assertTrue("Registration with existing username must fail", result.isFailure)
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(
            "Error should state username already exists: $message",
            message.contains("Username already exists", ignoreCase = true)
        )
    }

    @Test
    fun testRegister_shortPassword_returnsFailure() = runBlocking {
        val timestamp = System.currentTimeMillis()
        val result = authRepository.register(
            fullName = "Short Pass User",
            phone = null,
            email = "shortpass_$timestamp@gmail.com",
            username = "shortpass_$timestamp",
            password = "12"
        )
        assertTrue("Registration with short password must fail", result.isFailure)
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(
            "Error should state password length: $message",
            message.contains("at least 4 characters", ignoreCase = true) || message.contains("Password", ignoreCase = true)
        )
    }

    @Test
    fun testRegistration_and_login_lifecycle_with_profile_retrieval() = runBlocking {
        val timestamp = System.currentTimeMillis()
        val uniqueUsername = "user_$timestamp"
        val uniqueEmail = "user_$timestamp@gmail.com"
        val testPassword = "validPassword123"
        val testFullName = "Automated Test User"

        // 1. Register new user with 5 required fields
        val regResult = authRepository.register(
            fullName = testFullName,
            phone = "0912345678",
            email = uniqueEmail,
            username = uniqueUsername,
            password = testPassword
        )
        assertTrue("Registration must succeed: ${regResult.exceptionOrNull()?.message}", regResult.isSuccess)
        val member = regResult.getOrNull()
        assertNotNull(member)
        assertEquals(testFullName, member!!.name)

        // 2. Verify registration does NOT log the user in automatically
        assertNull("Registration must not automatically set token", fakeSessionRepository.getCachedToken())

        // 3. Login with newly created user with INCORRECT password -> must fail
        val wrongPassResult = authRepository.login(uniqueUsername, "wrongPass1234")
        assertTrue("Login with incorrect password must fail", wrongPassResult.isFailure)

        // 4. Login with newly created user with CORRECT password -> must succeed
        val loginResult = authRepository.login(uniqueUsername, testPassword)
        assertTrue("Login must succeed: ${loginResult.exceptionOrNull()?.message}", loginResult.isSuccess)
        val session = loginResult.getOrNull()
        assertNotNull(session)
        assertEquals(uniqueUsername, session!!.username)
        assertTrue("Token must be a valid non-empty JWT", session.token.startsWith("eyJ"))

        // 5. Verify token was cached in SessionRepository
        assertEquals("Cached token must match session token", session.token, fakeSessionRepository.getCachedToken())

        // 6. Test GET /api/auth/me profile endpoint with Bearer token
        val profileResult = authRepository.getProfile()
        assertTrue("Get profile must succeed: ${profileResult.exceptionOrNull()?.message}", profileResult.isSuccess)
        val profileSession = profileResult.getOrNull()!!
        assertEquals(testFullName, profileSession.fullName)
        assertEquals(uniqueEmail, profileSession.email)
        assertEquals(uniqueUsername, profileSession.username)

        // 7. Test protected request with injected Bearer token
        val protectedResponse = apiService.getAllCustomers()
        assertTrue("Authenticated request to /customers must return HTTP 200", protectedResponse.isSuccessful)
        assertEquals(200, protectedResponse.code())

        // 8. Test logout
        authRepository.logout()
        assertNull("Logout must clear the cached token", fakeSessionRepository.getCachedToken())

        // 9. Test protected request after logout (without token)
        val unauthResponse = apiService.getAllCustomers()
        assertEquals("Request without token must return HTTP 401 Unauthorized", 401, unauthResponse.code())
    }
}
