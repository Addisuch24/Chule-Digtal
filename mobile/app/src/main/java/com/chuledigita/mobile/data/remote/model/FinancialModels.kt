package com.chuledigita.mobile.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Request payload for creating a generic financial movement.
 */
data class CreateFinancialMovementRequest(
    val customerId: Long,
    val amount: Double,
    val movementType: String, // "DEBIT" or "CREDIT"
    val description: String?,
    val referenceType: String? = null,
    val referenceId: Long? = null,
    val transactionAt: String? = null,
    val idempotencyKey: String? = null
)

/**
 * Request payload for verifying or rejecting a financial movement/order.
 */
data class VerificationRequest(
    val verificationNote: String
)

/**
 * Financial movement DTO matching backend financial_movements table schema.
 */
data class FinancialMovementDto(
    val id: Long,
    @SerializedName("customer_id") val customerId: Long,
    @SerializedName("customer_name") val customerName: String?,
    val amount: Double,
    @SerializedName("movement_type") val movementType: String,
    val description: String?,
    @SerializedName("reference_type") val referenceType: String?,
    @SerializedName("reference_id") val referenceId: Long?,
    @SerializedName("recorded_by") val recordedBy: Long,
    @SerializedName("recorded_by_name") val recordedByName: String?,
    @SerializedName("verification_status") val verificationStatus: String,
    @SerializedName("verified_by") val verifiedBy: Long?,
    @SerializedName("verified_by_name") val verifiedByName: String?,
    @SerializedName("verified_at") val verifiedAt: String?,
    @SerializedName("verification_note") val verificationNote: String?,
    @SerializedName("transaction_at") val transactionAt: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?
)

/**
 * Response payload for single financial movement.
 */
data class FinancialMovementResponse(
    val success: Boolean,
    val message: String?,
    val data: FinancialMovementDto?
)

/**
 * Response payload for a list of financial movements.
 */
data class FinancialMovementListResponse(
    val success: Boolean,
    val message: String?,
    val data: List<FinancialMovementDto>
)
