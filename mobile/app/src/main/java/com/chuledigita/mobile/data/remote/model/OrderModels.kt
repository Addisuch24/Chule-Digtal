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
 * Request payload for creating an order with a title matching web behavior.
 */
data class CreateOrderDirectRequest(
    val orderNumber: String,
    val title: String,
    val description: String,
    val customerId: Long? = null
)

/**
 * Request payload for updating order title and description.
 */
data class UpdateOrderRequest(
    val title: String?,
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
    val title: String? = null,
    @SerializedName("customer_id") val customerId: Long? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("order_number") val orderNumber: String,
    val description: String? = null,
    @SerializedName("order_status") val orderStatus: String? = null,
    @SerializedName("verification_status") val verificationStatus: String? = null,
    @SerializedName("taken_by") val takenBy: Long? = null,
    @SerializedName("taken_by_name") val takenByName: String? = null,
    @SerializedName("verified_by") val verifiedBy: Long? = null,
    @SerializedName("verified_by_name") val verifiedByName: String? = null,
    @SerializedName("verified_at") val verifiedAt: String? = null,
    @SerializedName("verification_note") val verificationNote: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
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
