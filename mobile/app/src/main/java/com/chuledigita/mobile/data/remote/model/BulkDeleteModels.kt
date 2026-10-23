package com.chuledigita.mobile.data.remote.model

import com.google.gson.annotations.SerializedName

data class BulkDeleteNotesRequest(
    @SerializedName("noteIds")
    val noteIds: List<Long>,
    @SerializedName("ids")
    val ids: List<Long> = noteIds
)

data class BulkDeleteOrdersRequest(
    @SerializedName("orderIds")
    val orderIds: List<Long>,
    @SerializedName("ids")
    val ids: List<Long> = orderIds
)

data class BulkDeleteMovementsRequest(
    @SerializedName("movementIds")
    val movementIds: List<Long>,
    @SerializedName("ids")
    val ids: List<Long> = movementIds
)

data class BulkDeleteCustomersRequest(
    @SerializedName("customerIds")
    val customerIds: List<Long>,
    @SerializedName("ids")
    val ids: List<Long> = customerIds
)

data class BulkDeleteNotificationsRequest(
    @SerializedName("notificationIds")
    val notificationIds: List<Long>,
    @SerializedName("ids")
    val ids: List<Long> = notificationIds
)

data class SkippedItemDto(
    val id: Long,
    val reason: String
)

data class BulkDeleteData(
    val deletedCount: Int = 0,
    val deletedIds: List<Long> = emptyList(),
    val skipped: List<SkippedItemDto>? = null,
    val skippedCount: Int? = null
)

data class BulkDeleteResponse(
    val success: Boolean,
    val message: String,
    val data: BulkDeleteData? = null
)
