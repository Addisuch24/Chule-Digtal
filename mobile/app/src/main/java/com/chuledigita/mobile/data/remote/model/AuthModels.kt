package com.chuledigita.mobile.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Request payload for POST /api/auth/login endpoint.
 */
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * Request payload for POST /api/auth/register endpoint.
 * Note: Field names match backend's expectation in req.body.
 */
data class RegisterRequest(
    val fullName: String,
    val phone: String?,
    val username: String,
    val password: String
)

/**
 * User DTO object inside backend login and register response data.
 */
data class UserDto(
    val id: Long,
    @SerializedName("full_name") val fullName: String,
    val phone: String?,
    val username: String,
    @SerializedName("is_active") val isActive: Int,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

/**
 * Data payload inside backend login response.
 */
data class AuthDataDto(
    val token: String,
    val user: UserDto
)

/**
 * Response payload returned from POST /api/auth/login endpoint.
 */
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val data: AuthDataDto?
)

/**
 * Response payload returned from POST /api/auth/register endpoint.
 */
data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val data: UserDto?
)

/**
 * Standard backend error response structure.
 */
data class ApiErrorResponse(
    val success: Boolean?,
    val message: String?
)
