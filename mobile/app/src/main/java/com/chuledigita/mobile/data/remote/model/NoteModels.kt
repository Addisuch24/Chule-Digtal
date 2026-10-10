package com.chuledigita.mobile.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Request payload for creating a note (business or customer).
 */
data class CreateNoteRequest(
    @SerializedName("note_text") val noteText: String
)

/**
 * Customer Note DTO model matching backend customer_notes schema.
 */
data class NoteDto(
    val id: Long,
    @SerializedName("customer_id") val customerId: Long,
    @SerializedName("note_text") val noteText: String,
    @SerializedName("created_by") val createdBy: Long,
    @SerializedName("created_by_name") val createdByName: String?,
    @SerializedName("verification_status") val verificationStatus: String?,
    @SerializedName("verified_by") val verifiedBy: Long?,
    @SerializedName("verified_at") val verifiedAt: String?,
    @SerializedName("verification_note") val verificationNote: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?
)

/**
 * Global Business Note DTO model matching backend business_notes schema.
 */
data class BusinessNoteDto(
    val id: Long,
    @SerializedName("note_text") val noteText: String,
    @SerializedName("created_by") val createdBy: Long,
    @SerializedName("created_by_name") val createdByName: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?
)

/**
 * Response payload returned when a customer note is created.
 */
data class NoteResponse(
    val success: Boolean,
    val message: String,
    val data: NoteDto?
)

/**
 * Response payload returned when retrieving a list of customer notes.
 */
data class NoteListResponse(
    val success: Boolean,
    val message: String,
    val data: List<NoteDto>
)

/**
 * Response payload returned when a global business note is created.
 */
data class BusinessNoteResponse(
    val success: Boolean,
    val message: String,
    val data: BusinessNoteDto?
)

/**
 * Response payload returned when retrieving global business notes.
 */
data class BusinessNoteListResponse(
    val success: Boolean,
    val message: String,
    val data: List<BusinessNoteDto>
)
