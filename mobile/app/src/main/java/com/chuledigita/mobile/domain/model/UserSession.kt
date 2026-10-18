package com.chuledigita.mobile.domain.model

/**
 * Domain model representing an authenticated user session in Chule Digital.
 * Maps fields returned from the backend authentication API.
 */
data class UserSession(
    val id: Long,
    val fullName: String,
    val phone: String,
    val email: String? = null,
    val username: String,
    val isActive: Boolean,
    val token: String
)
