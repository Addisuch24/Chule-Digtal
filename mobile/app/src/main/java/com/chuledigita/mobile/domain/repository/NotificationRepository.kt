package com.chuledigita.mobile.domain.repository

import com.chuledigita.mobile.domain.model.NotificationItem
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain repository for managing business notifications and real-time event synchronization.
 */
interface NotificationRepository {
    /**
     * Flow of all notifications, synchronized with the server.
     */
    val notifications: StateFlow<List<NotificationItem>>

    /**
     * Flow of unread notification count.
     */
    val unreadCount: StateFlow<Int>

    /**
     * Shared flow emitting real-time notification events as they arrive over Socket.IO.
     */
    val realtimeEventFlow: SharedFlow<NotificationItem>

    /**
     * In-app pop-up notification manager.
     */
    val inAppNotificationManager: com.chuledigita.mobile.notifications.InAppNotificationManager

    /**
     * Fetches fresh notifications from the backend.
     */
    suspend fun refreshNotifications(): Result<Unit>

    /**
     * Marks a specific notification as read on the backend.
     */
    suspend fun markAsRead(notificationId: String): Result<Unit>

    /**
     * Marks all notifications as read for the current user.
     */
    suspend fun markAllAsRead(): Result<Unit>

    /**
     * Deletes a single notification for the current authenticated recipient.
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit>

    /**
     * Deletes multiple notifications for the current authenticated recipient.
     */
    suspend fun bulkDeleteNotifications(notificationIds: List<String>): Result<com.chuledigita.mobile.data.remote.model.BulkDeleteData>

    /**
     * Deletes all notifications for the current authenticated recipient.
     */
    suspend fun deleteAllNotifications(): Result<Unit>

    /**
     * Starts the real-time notification listener.
     */
    fun startRealTimeSync()

    /**
     * Stops the real-time notification listener.
     */
    fun stopRealTimeSync()
}
