package com.chuledigita.mobile.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Request payload for creating a new customer.
 */
data class CreateCustomerRequest(
    @SerializedName("full_name") val fullName: String,
    val phone: String?
)

/**
 * Customer DTO model matching backend database structure.
 */
data class CustomerDto(
    val id: Long,
    @SerializedName("full_name") val fullName: String,
    val phone: String?,
    @SerializedName("created_by") val createdBy: Long,
    @SerializedName("created_by_name") val createdByName: String?,
    @SerializedName("created_at") val createdAt: String
)

/**
 * Detailed Customer DTO model containing balance calculations.
 */
data class CustomerDetailDto(
    val id: Long,
    @SerializedName("full_name") val fullName: String,
    val phone: String?,
    @SerializedName("created_by") val createdBy: Long,
    @SerializedName("created_by_name") val createdByName: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    val totalDebit: Double,
    val totalCredit: Double,
    val balance: Double
)

/**
 * Response payload returned when creating a customer or fetching a single customer.
 */
data class CustomerResponse(
    val success: Boolean,
    val message: String,
    val data: CustomerDto?
)

/**
 * Response payload returned when fetching detailed customer information.
 */
data class CustomerDetailResponse(
    val success: Boolean,
    val message: String,
    val data: CustomerDetailDto?
)

/**
 * Response payload returned when fetching a list of all customers.
 */
data class CustomerListResponse(
    val success: Boolean,
    val message: String,
    val data: List<CustomerDto>
)

/**
 * Request payload for recording a customer payment.
 */
data class CreatePaymentRequest(
    val customerId: Long,
    val amount: Double,
    val description: String?,
    val referenceType: String?,
    val referenceId: Long?,
    val idempotencyKey: String?
)

/**
 * Response payload returned when a payment is recorded.
 */
data class PaymentResponse(
    val success: Boolean,
    val message: String,
    val data: FinancialMovementDto?
)

/**
 * Balance response payload returned from balance standalone endpoint.
 */
data class CustomerBalanceData(
    val totalDebit: Double,
    val totalCredit: Double,
    val balance: Double
)

data class CustomerBalanceResponse(
    val success: Boolean,
    val data: CustomerBalanceData?
)
