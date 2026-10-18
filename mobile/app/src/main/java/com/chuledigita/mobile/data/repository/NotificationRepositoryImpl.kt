package com.chuledigita.mobile.data.repository

import com.chuledigita.mobile.data.remote.ApiService
import com.chuledigita.mobile.data.remote.SocketManager
import com.chuledigita.mobile.data.remote.model.NotificationDto
import com.chuledigita.mobile.domain.model.NotificationItem
import com.chuledigita.mobile.domain.model.NotificationType
import com.chuledigita.mobile.domain.repository.NotificationRepository
import com.chuledigita.mobile.domain.repository.SessionRepository
import com.chuledigita.mobile.utils.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Implementation of [NotificationRepository] that orchestrates REST API sync
 * and real-time Socket.IO events.
 */
class NotificationRepositoryImpl(
    private val apiService: ApiService,
    private val socketManager: SocketManager,
    private val sessionRepository: SessionRepository
) : NotificationRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    override val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    override val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _realtimeEventFlow = MutableSharedFlow<NotificationItem>(replay = 1, extraBufferCapacity = 64)
    override val realtimeEventFlow: SharedFlow<NotificationItem> = _realtimeEventFlow.asSharedFlow()

    init {
        // 1. Session Lifecycle: Connect/Disconnect socket and fetch initial state
        scope.launch {
            sessionRepository.userSessionFlow.collectLatest { session ->
                if (session != null) {
                    socketManager.connect(session.token)
                    refreshNotifications()
                } else {
                    socketManager.disconnect()
                    _notifications.value = emptyList()
                    _unreadCount.value = 0
                }
            }
        }

        // 2. Reconnection Sync: Refresh from server whenever socket reconnects to catch missed events
        scope.launch {
            socketManager.connectionEvents.collect { event ->
                if (event == SocketManager.EVENT_CONNECTED) {
                    refreshNotifications()
                }
            }
        }
    }

    override suspend fun refreshNotifications(): Result<Unit> {
        return try {
            val session = sessionRepository.userSessionFlow.first()
                ?: return Result.failure(Exception("No active session"))

            val response = apiService.getNotifications(session.id.toLong())
            if (response.isSuccessful && response.body()?.success == true) {
                val dtoList = response.body()?.data ?: emptyList()
                val domainList = dtoList.map { it.toDomain() }

                _notifications.value = domainList
                updateUnreadCountLocally()
                Result.success(Unit)
            } else {
                _notifications.value = emptyList()
                updateUnreadCountLocally()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            _notifications.value = emptyList()
            updateUnreadCountLocally()
            Result.success(Unit)
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            val idLong = notificationId.toLongOrNull() ?: return Result.failure(Exception("Invalid ID"))
            val response = apiService.markAsRead(idLong)
            if (response.isSuccessful && response.body()?.success == true) {
                _notifications.value = _notifications.value.map {
                    if (it.id == notificationId) it.copy(isRead = true) else it
                }
                updateUnreadCountLocally()
                Result.success(Unit)
            } else {
                _notifications.value = _notifications.value.map {
                    if (it.id == notificationId) it.copy(isRead = true) else it
                }
                updateUnreadCountLocally()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            _notifications.value = _notifications.value.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }
            updateUnreadCountLocally()
            Result.success(Unit)
        }
    }

    override fun startRealTimeSync() {
        scope.launch {
            socketManager.notificationsFlow.collect { json ->
                handleIncomingSocketNotification(json)
            }
        }
    }

    override fun stopRealTimeSync() {
        // Cleanup handled via session flow and SocketManager.disconnect()
    }

    private fun handleIncomingSocketNotification(json: JSONObject) {
        try {
            val id = json.optLong("id", System.currentTimeMillis())
            val title = json.optString("title", "Notification")
            val message = json.optString("message", "")
            val typeStr = json.optString("notification_type", json.optString("notificationType", "SYSTEM"))
            val createdAt = json.optString("created_at", json.optString("createdAt", ""))

            val type = try {
                NotificationType.valueOf(typeStr)
            } catch (e: Exception) {
                NotificationType.SYSTEM
            }

            val newItem = NotificationItem(
                id = id.toString(),
                title = title,
                message = message,
                date = DateTimeUtils.formatDate(createdAt),
                time = DateTimeUtils.formatTime(createdAt),
                isRead = false,
                type = type
            )

            if (_notifications.value.none { it.id == newItem.id }) {
                _notifications.value = (listOf(newItem) + _notifications.value)
                    .distinctBy { it.id }
                    .sortedByDescending { it.id.toLongOrNull() ?: 0L }
                updateUnreadCountLocally()
            }

            // Emit real-time notification event so active feature ViewModels can refresh REST data
            _realtimeEventFlow.tryEmit(newItem)
        } catch (e: Exception) {
            println("DEBUG handleIncomingSocketNotification Exception: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun updateUnreadCountLocally() {
        _unreadCount.value = _notifications.value.count { !it.isRead }
    }

    private fun NotificationDto.toDomain(): NotificationItem {
        val type = try {
            NotificationType.valueOf(this.notificationType)
        } catch (e: Exception) {
            NotificationType.SYSTEM
        }

        return NotificationItem(
            id = this.id.toString(),
            title = this.title,
            message = this.message,
            date = DateTimeUtils.formatDate(this.createdAt),
            time = DateTimeUtils.formatTime(this.createdAt),
            isRead = this.isRead == 1,
            type = type,
            activityId = this.activityId?.toString()
        )
    }
}
