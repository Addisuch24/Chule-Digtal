package com.chuledigita.mobile.data.repository

import com.chuledigita.mobile.data.remote.ApiService
import com.chuledigita.mobile.data.remote.model.ApiErrorResponse
import com.chuledigita.mobile.data.remote.model.LoginRequest
import com.chuledigita.mobile.data.remote.model.RegisterRequest
import com.chuledigita.mobile.domain.model.FamilyMember
import com.chuledigita.mobile.domain.model.UserSession
import com.chuledigita.mobile.domain.repository.AuthRepository
import com.chuledigita.mobile.domain.repository.SessionRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of [AuthRepository] performing JWT authentication via Retrofit [ApiService]
 * and managing local session state via [SessionRepository].
 */
class AuthRepositoryImpl(
    private val apiService: ApiService,
    private val sessionRepository: SessionRepository
) : AuthRepository {

    private val gson = Gson()

    override val currentSessionFlow: Flow<UserSession?> = sessionRepository.userSessionFlow

    override suspend fun login(username: String, password: String): Result<UserSession> {
        return try {
            val response = apiService.login(LoginRequest(username = username, password = password))

            if (response.isSuccessful) {
                val loginResponse = response.body()
                val authData = loginResponse?.data
                if (loginResponse != null && loginResponse.success && authData != null) {
                    val userDto = authData.user
                    val userSession = UserSession(
                        id = userDto.id,
                        fullName = userDto.fullName,
                        phone = userDto.phone ?: "",
                        username = userDto.username,
                        isActive = userDto.isActive == 1,
                        token = authData.token
                    )
                    // Store token and user details safely in DataStore
                    sessionRepository.saveSession(userSession)
                    Result.success(userSession)
                } else {
                    Result.failure(Exception(loginResponse?.message ?: "Login failed. Invalid response from server."))
                }
            } else {
                // Parse backend error body safely without logging credentials or token
                val errorJson = response.errorBody()?.string()
                val errorMessage = try {
                    val errorObj = gson.fromJson(errorJson, ApiErrorResponse::class.java)
                    errorObj.message ?: "Authentication failed (HTTP ${response.code()})"
                } catch (e: Exception) {
                    "Authentication failed (HTTP ${response.code()})"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(
                Exception("Unable to connect to backend server. Please verify your connection or server status.")
            )
        }
    }

    override suspend fun register(
        fullName: String,
        phone: String?,
        username: String,
        password: String
    ): Result<FamilyMember> {
        return try {
            val response = apiService.register(
                RegisterRequest(
                    fullName = fullName,
                    phone = phone,
                    username = username,
                    password = password
                )
            )

            if (response.isSuccessful) {
                val registerResponse = response.body()
                val userDto = registerResponse?.data
                if (registerResponse != null && registerResponse.success && userDto != null) {
                    val familyMember = FamilyMember(
                        id = userDto.id.toString(),
                        name = userDto.fullName,
                        role = if (userDto.isActive == 1) "Active Member" else "Inactive Member"
                    )
                    Result.success(familyMember)
                } else {
                    Result.failure(Exception(registerResponse?.message ?: "Registration failed. Invalid response from server."))
                }
            } else {
                val errorJson = response.errorBody()?.string()
                val errorMessage = try {
                    val errorObj = gson.fromJson(errorJson, ApiErrorResponse::class.java)
                    errorObj.message ?: "Registration failed (HTTP ${response.code()})"
                } catch (e: Exception) {
                    "Registration failed (HTTP ${response.code()})"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(
                Exception("Unable to connect to backend server. Please verify your connection or server status.")
            )
        }
    }

    override suspend fun logout() {
        sessionRepository.clearSession()
    }
}
