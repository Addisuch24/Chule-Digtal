package com.chuledigita.mobile.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Request payload for creating a new order.
 * Backend uses camelCase field for creation body.
 */
data class CreateOrderRequest(
    val orderNumber: String,
    val description: String?
)

/**
 * Request payload for updating order status.
 * Backend uses camelCase field for request body.
 */
data class UpdateOrderStatusRequest(
    val orderStatus: String
)

/**
 * Order DTO model matching backend database response structure.
 */
data class OrderDto(
    val id: Long,
    @SerializedName("customer_id") val customerId: Long,
    @SerializedName("customer_name") val customerName: String?,
    @SerializedName("order_number") val orderNumber: String,
    val description: String?,
    @SerializedName("order_status") val orderStatus: String,
    @SerializedName("verification_status") val verificationStatus: String,
    @SerializedName("taken_by") val takenBy: Long,
    @SerializedName("taken_by_name") val takenByName: String?,
    @SerializedName("verified_by") val verifiedBy: Long?,
    @SerializedName("verified_by_name") val verifiedByName: String?,
    @SerializedName("verified_at") val verifiedAt: String?,
    @SerializedName("verification_note") val verificationNote: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?
)

/**
 * Response payload returned when an order is created, fetched, or updated.
 */
data class OrderResponse(
    val success: Boolean,
    val message: String,
    val data: OrderDto?
)

/**
 * Response payload returned when fetching a list of all orders.
 */
data class OrderListResponse(
    val success: Boolean,
    val message: String,
    val data: List<OrderDto>
)
