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
 * Implementation of [AuthRepository] attempting real backend JWT authentication.
 */
class AuthRepositoryImpl(
    private val apiService: ApiService,
    private val sessionRepository: SessionRepository
) : AuthRepository {

    private val gson = Gson()

    override val currentSessionFlow: Flow<UserSession?> = sessionRepository.userSessionFlow

    override suspend fun login(username: String, password: String): Result<UserSession> {
        return try {
            val response = apiService.login(LoginRequest(username = username.trim(), password = password))
            println("DEBUG LOGIN RESP: code=${response.code()}, body=${response.body()}")

            if (response.isSuccessful) {
                val loginResponse = response.body()
                val authData = loginResponse?.data
                if (loginResponse != null && loginResponse.success && authData != null && !authData.token.isNullOrBlank()) {
                    val userDto = authData.user
                    val userSession = UserSession(
                        id = userDto.id,
                        fullName = userDto.fullName ?: userDto.username ?: username,
                        phone = userDto.phone ?: "",
                        email = userDto.email,
                        username = userDto.username ?: username,
                        isActive = userDto.isUserActive,
                        token = authData.token
                    )
                    sessionRepository.saveSession(userSession)
                    Result.success(userSession)
                } else {
                    Result.failure(Exception(loginResponse?.message ?: "Login failed"))
                }
            } else {
                val errorJson = response.errorBody()?.string()
                val errorMessage = try {
                    val errorObj = gson.fromJson(errorJson, ApiErrorResponse::class.java)
                    errorObj.message ?: when (response.code()) {
                        401 -> "Invalid username or password."
                        else -> "Authentication failed (HTTP ${response.code()})"
                    }
                } catch (_: Exception) {
                    when (response.code()) {
                        401 -> "Invalid username or password."
                        else -> "Authentication failed (HTTP ${response.code()})"
                    }
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error during login."))
        }
    }

    override suspend fun register(
        fullName: String,
        phone: String?,
        email: String,
        username: String,
        password: String
    ): Result<FamilyMember> {
        return try {
            val trimmedEmail = email.trim()
            val response = apiService.register(
                RegisterRequest(
                    fullName = fullName.trim(),
                    phone = phone?.trim()?.ifBlank { null },
                    email = trimmedEmail,
                    googleAccount = trimmedEmail,
                    username = username.trim(),
                    password = password
                )
            )

            if (response.isSuccessful) {
                val registerResponse = response.body()
                val userDto = registerResponse?.data
                if (registerResponse != null && registerResponse.success && userDto != null) {
                    val familyMember = FamilyMember(
                        id = userDto.id.toString(),
                        name = userDto.fullName ?: userDto.username ?: username,
                        role = if (userDto.isUserActive) "Active Member" else "Inactive Member"
                    )
                    Result.success(familyMember)
                } else {
                    Result.failure(Exception(registerResponse?.message ?: "Registration failed."))
                }
            } else {
                val errorJson = response.errorBody()?.string()
                val errorMessage = try {
                    val errorObj = gson.fromJson(errorJson, ApiErrorResponse::class.java)
                    errorObj.message ?: when (response.code()) {
                        409 -> "Username or Google account email already exists."
                        else -> "Registration failed (HTTP ${response.code()})"
                    }
                } catch (_: Exception) {
                    when (response.code()) {
                        409 -> "Username or Google account email already exists."
                        else -> "Registration failed (HTTP ${response.code()})"
                    }
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error during registration."))
        }
    }

    override suspend fun getProfile(): Result<UserSession> {
        return try {
            val response = apiService.getProfile()
            if (response.isSuccessful && response.body()?.success == true) {
                val userDto = response.body()!!.data!!
                val token = sessionRepository.getCachedToken() ?: ""
                val session = UserSession(
                    id = userDto.id,
                    fullName = userDto.fullName ?: userDto.username ?: "",
                    phone = userDto.phone ?: "",
                    email = userDto.email,
                    username = userDto.username ?: "",
                    isActive = userDto.isUserActive,
                    token = token
                )
                sessionRepository.saveSession(session)
                Result.success(session)
            } else {
                Result.failure(Exception("Failed to retrieve profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        sessionRepository.clearSession()
    }
}
