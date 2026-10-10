package com.chuledigita.mobile.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Notification DTO matching backend notifications table schema.
 * Note: REST API returns snake_case from DB columns.
 */
data class NotificationDto(
    val id: Long,
    @SerializedName("recipient_id") val recipientId: Long,
    @SerializedName("activity_id") val activityId: Long?,
    @SerializedName("notification_type") val notificationType: String,
    val title: String,
    val message: String,
    @SerializedName("is_read") val isRead: Int, // Backend returns 0/1 for booleans
    @SerializedName("read_at") val readAt: String?,
    @SerializedName("created_at") val createdAt: String
)

/**
 * Response payload for fetching notifications.
 */
data class NotificationListResponse(
    val success: Boolean,
    val message: String?,
    val data: List<NotificationDto>
)

/**
 * Response payload for a single notification.
 */
data class NotificationResponse(
    val success: Boolean,
    val message: String?,
    val data: NotificationDto?
)

/**
 * Response payload for unread count.
 */
data class UnreadCountResponse(
    val success: Boolean,
    @SerializedName("unread_count") val unreadCount: Int
)
