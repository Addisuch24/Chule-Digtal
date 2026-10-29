package com.chuledigita.mobile.data.repository

import android.content.Context
import android.util.Log
import com.chuledigita.mobile.data.remote.ApiService
import com.chuledigita.mobile.data.remote.SocketManager
import com.chuledigita.mobile.data.remote.model.NotificationDto
import com.chuledigita.mobile.domain.model.NotificationItem
import com.chuledigita.mobile.domain.model.NotificationType
import com.chuledigita.mobile.domain.model.UserSession
import com.chuledigita.mobile.domain.repository.NotificationRepository
import com.chuledigita.mobile.domain.repository.SessionRepository
import com.chuledigita.mobile.notifications.InAppNotificationManager
import com.chuledigita.mobile.notifications.SystemNotificationHelper
import com.chuledigita.mobile.utils.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Collections
import org.json.JSONObject

/**
 * Implementation of [NotificationRepository] that orchestrates REST API sync,
 * real-time Socket.IO events, foreground adaptive polling fallback, in-app pop-up presentation,
 * and background system notifications.
 */
class NotificationRepositoryImpl(
    private val apiService: ApiService,
    private val socketManager: SocketManager,
    private val sessionRepository: SessionRepository,
    private val context: Context? = null,
    override val inAppNotificationManager: InAppNotificationManager = InAppNotificationManager()
) : NotificationRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    override val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    override val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // Replay = 0 ensures new collectors do not receive stale replayed notification events
    private val _realtimeEventFlow = MutableSharedFlow<NotificationItem>(replay = 0, extraBufferCapacity = 64)
    override val realtimeEventFlow: SharedFlow<NotificationItem> = _realtimeEventFlow.asSharedFlow()

    // Thread-safe client-side tombstone set to prevent in-flight or replayed socket events from resurrecting deleted notifications
    val deletedNotificationIds: MutableSet<String> = Collections.synchronizedSet(mutableSetOf<String>())

    @Volatile
    private var activeSession: UserSession? = null

    @Volatile
    override var isForeground: Boolean = false
        private set

    var pollingJob: Job? = null
        private set

    var pollIntervalMs: Long = 30_000L

    init {
        // 1. Session Lifecycle: Connect/Disconnect socket and fetch initial state
        scope.launch {
            sessionRepository.userSessionFlow.collectLatest { session ->
                activeSession = session
                if (session != null) {
                    Log.d("NotificationRepo", "Session activated for user ID ${session.id} (${session.username})")
                    socketManager.connect(session.token)
                    refreshNotificationsInternal(isRecovery = false)
                    evaluateAdaptivePolling()
                } else {
                    Log.d("NotificationRepo", "Session deactivated - stopping polling and socket")
                    stopAdaptivePolling()
                    socketManager.disconnect()
                    inAppNotificationManager.clearSession()
                    deletedNotificationIds.clear()
                    _notifications.value = emptyList()
                    _unreadCount.value = 0
                }
            }
        }

        // 2. Reconnection Sync: Refresh from server whenever socket reconnects to catch missed events
        scope.launch {
            socketManager.connectionEvents.collect { event ->
                when (event) {
                    SocketManager.EVENT_CONNECTED -> {
                        Log.i("NotificationRepo", "Socket connected - suspending adaptive polling")
                        stopAdaptivePolling()
                        refreshNotificationsInternal(isRecovery = true)
                    }
                    SocketManager.EVENT_DISCONNECTED, SocketManager.EVENT_UNAVAILABLE -> {
                        Log.i("NotificationRepo", "Socket event: $event - evaluating adaptive polling")
                        evaluateAdaptivePolling()
                    }
                }
            }
        }

        // 3. Multi-Device Real-time Deletion Synchronization
        scope.launch {
            socketManager.notificationDeletedFlow.collect { deletedId ->
                deletedNotificationIds.add(deletedId)
                _notifications.value = _notifications.value.filter { it.id != deletedId }
                updateUnreadCountLocally()
            }
        }

        scope.launch {
            socketManager.notificationBulkDeletedFlow.collect { deletedIds ->
                deletedNotificationIds.addAll(deletedIds)
                _notifications.value = _notifications.value.filter { it.id !in deletedIds }
                updateUnreadCountLocally()
            }
        }

        scope.launch {
            socketManager.notificationAllDeletedFlow.collect {
                _notifications.value = emptyList()
                updateUnreadCountLocally()
            }
        }
    }

    override suspend fun refreshNotifications(): Result<Unit> {
        return refreshNotificationsInternal(isRecovery = false)
    }

    private suspend fun refreshNotificationsInternal(isRecovery: Boolean): Result<Unit> {
        return try {
            val session = activeSession ?: sessionRepository.userSessionFlow.first()
            if (session == null) {
                Log.w("NotificationRepo", "refreshNotificationsInternal skipped: No active session")
                return Result.failure(Exception("No active session"))
            }

            Log.d("NotificationRepo", "Requesting notifications from server for recipientId: ${session.id} (isRecovery=$isRecovery)")
            val response = apiService.getNotifications(session.id.toLong())
            if (response.isSuccessful && response.body()?.success == true) {
                val dtoList = response.body()?.data ?: emptyList()
                Log.i("NotificationRepo", "Received ${dtoList.size} notifications from server")
                val serverList = dtoList.map { it.toDomain() }

                // Filter out any notifications that have been persistently deleted in this session
                val filteredList = serverList.filter { it.id !in deletedNotificationIds }

                // Detect unread notifications that were not yet presented in this session
                val unreadRecovery = filteredList.filter { !it.isRead && !inAppNotificationManager.presentedNotificationIds.contains(it.id) }

                // Update local state with authoritative list from server (removes deleted notifications)
                val previousIds = _notifications.value.map { it.id }.toSet()
                _notifications.value = filteredList.sortedByDescending { it.id.toLongOrNull() ?: 0L }

                updateUnreadCountLocally()

                // Notify feature listeners (e.g. Dashboard) of newly arrived notifications
                val newArrivals = filteredList.filter { it.id !in previousIds }
                if (newArrivals.isNotEmpty()) {
                    Log.i("NotificationRepo", "Detected ${newArrivals.size} new notifications; emitting to realtimeEventFlow")
                    for (newItem in newArrivals) {
                        _realtimeEventFlow.tryEmit(newItem)
                    }
                }

                // If recovering after offline/reconnect and unread items exist, present them controlled
                if (isRecovery && unreadRecovery.isNotEmpty()) {
                    Log.i("NotificationRepo", "Presenting ${unreadRecovery.size} unread notifications via inAppNotificationManager")
                    inAppNotificationManager.showBatchFromRecovery(unreadRecovery)
                }

                Result.success(Unit)
            } else {
                val code = response.code()
                val errorBody = response.errorBody()?.string()
                Log.e("NotificationRepo", "Failed to fetch notifications: HTTP $code - $errorBody")
                updateUnreadCountLocally()
                Result.failure(Exception("HTTP $code: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Exception while fetching notifications: ${e.message}", e)
            // Preserve existing _notifications on error to maintain offline availability
            updateUnreadCountLocally()
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            val idLong = notificationId.toLongOrNull() ?: return Result.failure(Exception("Invalid ID"))
            val response = apiService.markAsRead(idLong)
            if (response.isSuccessful) {
                _notifications.value = _notifications.value.map {
                    if (it.id == notificationId) it.copy(isRead = true) else it
                }
                updateUnreadCountLocally()
                Result.success(Unit)
            } else {
                // Optimistic local update
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

    override suspend fun markAllAsRead(): Result<Unit> {
        return try {
            val response = apiService.markAllAsRead()
            if (response.isSuccessful) {
                _notifications.value = _notifications.value.map { it.copy(isRead = true) }
                updateUnreadCountLocally()
                Result.success(Unit)
            } else {
                _notifications.value = _notifications.value.map { it.copy(isRead = true) }
                updateUnreadCountLocally()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            _notifications.value = _notifications.value.map { it.copy(isRead = true) }
            updateUnreadCountLocally()
            Result.success(Unit)
        }
    }

    override suspend fun bulkDeleteNotifications(notificationIds: List<String>): Result<com.chuledigita.mobile.data.remote.model.BulkDeleteData> {
        return try {
            val validLongs = notificationIds.mapNotNull { it.toLongOrNull() }
            if (validLongs.isEmpty()) {
                return Result.failure(Exception("No valid notification IDs provided"))
            }
            val request = com.chuledigita.mobile.data.remote.model.BulkDeleteNotificationsRequest(notificationIds = validLongs)
            val response = apiService.bulkDeleteNotifications(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data ?: com.chuledigita.mobile.data.remote.model.BulkDeleteData(deletedCount = 0)
                val actuallyDeleted = data.deletedIds.map { it.toString() }.ifEmpty { notificationIds }
                deletedNotificationIds.addAll(actuallyDeleted)
                // Remove deleted notifications from local state and update unread count
                _notifications.value = _notifications.value.filter { it.id !in actuallyDeleted }
                updateUnreadCountLocally()
                // Refresh authoritative list from server
                refreshNotificationsInternal(isRecovery = false)
                Result.success(data)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "Failed to delete notifications")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error while deleting notifications: ${e.message}"))
        }
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            val idLong = notificationId.toLongOrNull() ?: return Result.failure(Exception("Invalid ID"))
            val response = apiService.deleteNotification(idLong)
            if (response.isSuccessful) {
                deletedNotificationIds.add(notificationId)
                _notifications.value = _notifications.value.filter { it.id != notificationId }
                updateUnreadCountLocally()
                // Refresh authoritative list from server
                refreshNotificationsInternal(isRecovery = false)
                Result.success(Unit)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "Failed to delete notification")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error while deleting notification: ${e.message}"))
        }
    }

    override suspend fun deleteAllNotifications(): Result<Unit> {
        return try {
            val response = apiService.deleteAllNotifications()
            if (response.isSuccessful) {
                _notifications.value = emptyList()
                updateUnreadCountLocally()
                // Refresh authoritative list from server
                refreshNotificationsInternal(isRecovery = false)
                Result.success(Unit)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "Failed to delete all notifications")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error while deleting all notifications: ${e.message}"))
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
        stopAdaptivePolling()
        socketManager.disconnect()
    }

    override fun onAppForeground() {
        Log.i("NotificationRepo", "App entered foreground (onAppForeground)")
        isForeground = true
        evaluateAdaptivePolling()
        scope.launch {
            // Immediate refresh on resume to capture any updates created while backgrounded
            Log.d("NotificationRepo", "Triggering immediate resume refresh")
            refreshNotificationsInternal(isRecovery = true)
        }
    }

    override fun onAppBackground() {
        Log.i("NotificationRepo", "App entered background (onAppBackground)")
        isForeground = false
        stopAdaptivePolling()
    }

    @Synchronized
    fun evaluateAdaptivePolling() {
        val session = activeSession ?: runCatching { kotlinx.coroutines.runBlocking { sessionRepository.userSessionFlow.first() } }.getOrNull()
        val hasSession = session != null || sessionRepository.getCachedToken() != null
        val socketActive = socketManager.isConnected()
        val socketSupported = socketManager.isSocketSupported()

        Log.d(
            "AdaptivePolling",
            "evaluateAdaptivePolling -> isForeground=$isForeground, hasSession=$hasSession, socketActive=$socketActive, socketSupported=$socketSupported"
        )

        // Polling is active only in the foreground when logged in and Socket.IO is not connected
        if (isForeground && hasSession && !socketActive) {
            startAdaptivePolling()
        } else {
            stopAdaptivePolling()
        }
    }

    @Synchronized
    fun startAdaptivePolling() {
        if (pollingJob?.isActive == true) {
            Log.d("AdaptivePolling", "startAdaptivePolling skipped: job already active")
            return
        }
        Log.i("AdaptivePolling", "Starting foreground adaptive polling (interval: ${pollIntervalMs}ms)")
        pollingJob = scope.launch {
            while (isActive && isForeground && !socketManager.isConnected()) {
                delay(pollIntervalMs)
                if (!isActive || !isForeground || socketManager.isConnected()) break
                Log.d("AdaptivePolling", "Adaptive polling tick: checking for new notifications")
                refreshNotificationsInternal(isRecovery = true)
            }
            Log.i("AdaptivePolling", "Foreground adaptive polling loop ended")
        }
    }

    @Synchronized
    fun stopAdaptivePolling() {
        if (pollingJob != null) {
            Log.i("AdaptivePolling", "Stopping foreground adaptive polling")
            pollingJob?.cancel()
            pollingJob = null
        }
    }

    fun handleIncomingSocketNotification(json: JSONObject) {
        try {
            // 1. Authorization check: confirm recipient belongs to authenticated session
            val session = activeSession ?: runCatching { kotlinx.coroutines.runBlocking { sessionRepository.userSessionFlow.first() } }.getOrNull()
            val recipientId = json.optLong("recipient_id", json.optLong("recipientId", -1L))
            if (recipientId != -1L && session != null) {
                if (recipientId != session.id) {
                    Log.w("NotificationRepo", "Dropped notification for another recipient: $recipientId (active: ${session.id})")
                    return
                }
            }

            // 2. Parse persistent ID
            val idRaw = json.opt("id")
            val idStr = when (idRaw) {
                is Number -> idRaw.toLong().toString()
                is String -> idRaw.trim()
                else -> ""
            }
            if (idStr.isEmpty() || idStr == "-1") {
                Log.w("NotificationRepo", "Missing notification ID in socket payload; dropping")
                return
            }

            // 3. Drop notification if it was persistently deleted on this client
            if (deletedNotificationIds.contains(idStr)) {
                Log.d("NotificationRepo", "Notification $idStr was deleted; ignoring incoming socket notification")
                return
            }

            // 4. Deduplication check against local state
            if (_notifications.value.any { it.id == idStr }) {
                Log.d("NotificationRepo", "Duplicate notification $idStr ignored")
                return
            }

            val title = json.optString("title", "Business Update").ifBlank { "Business Update" }
            val message = json.optString("message", "")
            val typeStr = json.optString("notification_type", json.optString("notificationType", "SYSTEM"))
            val createdAt = json.optString("created_at", json.optString("createdAt", ""))
            val entityType = json.optString("entity_type", json.optString("entityType", "")).ifBlank { null }
            val entityId = json.optString("entity_id", json.optString("entityId", "")).ifBlank { null }
            val activityId = json.optString("activity_id", json.optString("activityId", "")).ifBlank { null }

            val type = try {
                NotificationType.valueOf(typeStr)
            } catch (e: Exception) {
                NotificationType.SYSTEM
            }

            val newItem = NotificationItem(
                id = idStr,
                title = title,
                message = message,
                date = DateTimeUtils.formatDate(createdAt),
                time = DateTimeUtils.formatTime(createdAt),
                isRead = false,
                type = type,
                activityId = activityId,
                entityType = entityType,
                entityId = entityId
            )

            // 4. Update local state
            _notifications.value = (listOf(newItem) + _notifications.value)
                .distinctBy { it.id }
                .sortedByDescending { it.id.toLongOrNull() ?: 0L }
            updateUnreadCountLocally()

            // 5. Emit real-time notification event so active feature ViewModels can refresh REST data
            _realtimeEventFlow.tryEmit(newItem)

            // 6. Presentation: In-app pop-up if foreground; System notification if background
            if (isForeground) {
                inAppNotificationManager.showNotification(newItem)
            } else {
                context?.let { ctx ->
                    SystemNotificationHelper.postSystemNotificationIfBackground(ctx, newItem)
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error processing socket notification: ${e.message}", e)
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
            isRead = this.isRead,
            type = type,
            activityId = this.activityId?.toString(),
            entityType = this.entityType,
            entityId = this.entityId
        )
    }

    private fun parseErrorMessage(errorBodyJson: String?, fallback: String): String {
        if (errorBodyJson.isNullOrBlank()) return fallback
        return try {
            val json = JSONObject(errorBodyJson)
            json.optString("message", fallback)
        } catch (e: Exception) {
            fallback
        }
    }
}
