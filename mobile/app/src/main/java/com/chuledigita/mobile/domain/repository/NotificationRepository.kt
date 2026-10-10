package com.chuledigita.mobile.domain.repository

import com.chuledigita.mobile.domain.model.NotificationItem
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain repository for managing business notifications.
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
     * Fetches fresh notifications from the backend.
     */
    suspend fun refreshNotifications(): Result<Unit>

    /**
     * Marks a specific notification as read on the backend.
     */
    suspend fun markAsRead(notificationId: String): Result<Unit>

    /**
     * Starts the real-time notification listener.
     */
    fun startRealTimeSync()

    /**
     * Stops the real-time notification listener.
     */
    fun stopRealTimeSync()
}
