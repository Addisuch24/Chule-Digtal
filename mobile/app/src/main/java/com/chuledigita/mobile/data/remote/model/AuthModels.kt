package com.chuledigita.mobile.data.remote.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/**
 * Request payload for POST /api/auth/login endpoint.
 */
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * Request payload for POST /api/auth/register endpoint matching the 5 required fields.
 */
data class RegisterRequest(
    val fullName: String,
    val phone: String?,
    val email: String?,
    val googleAccount: String?,
    val username: String,
    val password: String
)

/**
 * User DTO object inside backend login and register response data.
 */
data class UserDto(
    val id: Long = 0L,
    @SerializedName("full_name") val fullName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val username: String? = null,
    @SerializedName("is_active") val rawIsActive: JsonElement? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
) {
    val isUserActive: Boolean
        get() {
            return try {
                when {
                    rawIsActive == null || rawIsActive.isJsonNull -> true
                    rawIsActive.isJsonPrimitive && rawIsActive.asJsonPrimitive.isBoolean -> rawIsActive.asBoolean
                    rawIsActive.isJsonPrimitive && rawIsActive.asJsonPrimitive.isNumber -> rawIsActive.asInt == 1
                    else -> true
                }
            } catch (_: Exception) {
                true
            }
        }
}

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
 * Response payload returned from GET /api/auth/me profile endpoint.
 */
data class ProfileResponse(
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
