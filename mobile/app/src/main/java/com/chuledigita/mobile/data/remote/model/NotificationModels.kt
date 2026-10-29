package com.chuledigita.mobile.data.remote.model

import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * TypeAdapter that safely handles boolean values serialized as boolean (true/false),
 * integer/number (1/0), string ("true"/"false"/"1"/"0"), or null.
 */
class BooleanLenientAdapter : TypeAdapter<Boolean>() {
    override fun write(out: JsonWriter, value: Boolean?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value)
        }
    }

    override fun read(reader: JsonReader): Boolean {
        return when (reader.peek()) {
            JsonToken.BOOLEAN -> reader.nextBoolean()
            JsonToken.NUMBER -> reader.nextInt() != 0
            JsonToken.STRING -> {
                val str = reader.nextString().trim()
                str.equals("true", ignoreCase = true) || str == "1"
            }
            JsonToken.NULL -> {
                reader.nextNull()
                false
            }
            else -> {
                reader.skipValue()
                false
            }
        }
    }
}

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
    @SerializedName("is_read")
    @JsonAdapter(BooleanLenientAdapter::class)
    val isRead: Boolean = false,
    @SerializedName("read_at") val readAt: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("entity_type") val entityType: String? = null,
    @SerializedName("entity_id") val entityId: String? = null
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
