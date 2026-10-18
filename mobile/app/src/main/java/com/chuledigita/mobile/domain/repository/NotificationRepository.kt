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
