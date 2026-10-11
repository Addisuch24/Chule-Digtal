package com.chuledigita.mobile.domain.model

/**
 * Domain model representing a family member in Chule Digital family business.
 * Every business action in the app identifies the family member who performed it.
 */
data class FamilyMember(
    val id: String,
    val name: String,
    val role: String // e.g. "Admin", "Manager", "Member"
)
