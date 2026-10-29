package com.chuledigita.mobile.data.remote

import android.util.Log
import com.chuledigita.mobile.BuildConfig
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
import java.net.URISyntaxException

/**
 * Manages the lifecycle and events of the Socket.IO real-time connection.
 */
class SocketManager(private val baseUrl: String) {

    private var socket: Socket? = null
    
    private val _notificationsFlow = MutableSharedFlow<JSONObject>(replay = 0, extraBufferCapacity = 64)
    val notificationsFlow: SharedFlow<JSONObject> = _notificationsFlow.asSharedFlow()

    private val _notificationDeletedFlow = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64)
    val notificationDeletedFlow: SharedFlow<String> = _notificationDeletedFlow.asSharedFlow()

    private val _notificationBulkDeletedFlow = MutableSharedFlow<List<String>>(replay = 0, extraBufferCapacity = 64)
    val notificationBulkDeletedFlow: SharedFlow<List<String>> = _notificationBulkDeletedFlow.asSharedFlow()

    private val _notificationAllDeletedFlow = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 64)
    val notificationAllDeletedFlow: SharedFlow<Unit> = _notificationAllDeletedFlow.asSharedFlow()

    private val _dashboardRefreshFlow = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 64)
    val dashboardRefreshFlow: SharedFlow<Unit> = _dashboardRefreshFlow.asSharedFlow()

    private val _connectionEvents = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val connectionEvents: SharedFlow<String> = _connectionEvents.asSharedFlow()

    companion object {
        const val EVENT_CONNECTED = "connected"
        const val EVENT_DISCONNECTED = "disconnected"
        const val EVENT_RECONNECTING = "reconnecting"
        const val EVENT_UNAVAILABLE = "unavailable"

        /**
         * Checks whether the target backend URL is a known serverless host (e.g. Vercel)
         * where persistent WebSocket / Socket.IO connections are not supported.
         */
        fun isServerlessBackend(url: String): Boolean {
            val clean = url.lowercase().trim()
            return clean.contains(".vercel.app")
        }
    }

    /**
     * Checks if the configured backend supports persistent Socket.IO connections.
     */
    fun isSocketSupported(): Boolean {
        val socketUrl = baseUrl.removeSuffix("api/").removeSuffix("/")
        return !isServerlessBackend(socketUrl)
    }

    private fun logDebug(tag: String, message: String) {
        runCatching {
            if (BuildConfig.DEBUG) Log.d(tag, message)
        }
    }

    private fun logError(tag: String, message: String, throwable: Throwable? = null) {
        runCatching {
            if (BuildConfig.DEBUG) Log.e(tag, message, throwable)
        }
    }

    /**
     * Connects to the socket server using the provided JWT token for authentication.
     */
    fun connect(token: String) {
        if (socket != null) return

        // Remove 'api/' suffix from base URL for socket connection if present
        val socketUrl = baseUrl.removeSuffix("api/").removeSuffix("/")

        // Guard against infinite failed connection retries on known serverless platforms (e.g. Vercel)
        if (isServerlessBackend(socketUrl)) {
            logDebug("SocketManager", "Target backend is a serverless host ($socketUrl); Socket.IO is unsupported. Entering standby.")
            _connectionEvents.tryEmit(EVENT_UNAVAILABLE)
            return
        }

        try {
            val options = IO.Options.builder()
                .setAuth(mapOf("token" to token))
                .setForceNew(true)
                .setReconnection(true)
                .setReconnectionAttempts(5)
                .setReconnectionDelay(2000)
                .setReconnectionDelayMax(10000)
                .build()

            socket = IO.socket(socketUrl, options)

            socket?.on(Socket.EVENT_CONNECT) {
                logDebug("SocketManager", "Connected to socket server")
                _connectionEvents.tryEmit(EVENT_CONNECTED)
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                logDebug("SocketManager", "Disconnected from socket server")
                _connectionEvents.tryEmit(EVENT_DISCONNECTED)
            }

            socket?.on("reconnect") {
                logDebug("SocketManager", "Reconnected to socket server")
                _connectionEvents.tryEmit(EVENT_CONNECTED)
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                logError("SocketManager", "Connection error: ${args.firstOrNull()}")
                _connectionEvents.tryEmit(EVENT_DISCONNECTED)
            }

            // Real-time notification event from verified backend contract
            socket?.on("notification:new") { args ->
                val data = args.firstOrNull() as? JSONObject
                if (data != null) {
                    logDebug("SocketManager", "Received notification: $data")
                    _notificationsFlow.tryEmit(data)
                }
            }

            socket?.on("notification:deleted") { args ->
                val data = args.firstOrNull() as? JSONObject
                val id = data?.optString("notificationId", data.optString("id", ""))
                if (!id.isNullOrBlank()) {
                    logDebug("SocketManager", "Received notification:deleted for $id")
                    _notificationDeletedFlow.tryEmit(id)
                }
            }

            socket?.on("notification:bulk_deleted") { args ->
                val data = args.firstOrNull() as? JSONObject
                val idsArray = data?.optJSONArray("notificationIds") ?: data?.optJSONArray("ids")
                val idsList = mutableListOf<String>()
                if (idsArray != null) {
                    for (i in 0 until idsArray.length()) {
                        idsList.add(idsArray.optString(i))
                    }
                }
                if (idsList.isNotEmpty()) {
                    logDebug("SocketManager", "Received notification:bulk_deleted for $idsList")
                    _notificationBulkDeletedFlow.tryEmit(idsList)
                }
            }

            socket?.on("notification:all_deleted") {
                logDebug("SocketManager", "Received notification:all_deleted")
                _notificationAllDeletedFlow.tryEmit(Unit)
            }

            // Real-time dashboard refresh and customer change events
            socket?.on("dashboard:refresh") {
                logDebug("SocketManager", "Received dashboard:refresh")
                _dashboardRefreshFlow.tryEmit(Unit)
            }
            socket?.on("customer:deleted") {
                logDebug("SocketManager", "Received customer:deleted")
                _dashboardRefreshFlow.tryEmit(Unit)
            }
            socket?.on("customer:created") {
                logDebug("SocketManager", "Received customer:created")
                _dashboardRefreshFlow.tryEmit(Unit)
            }
            socket?.on("customer:updated") {
                logDebug("SocketManager", "Received customer:updated")
                _dashboardRefreshFlow.tryEmit(Unit)
            }

            socket?.connect()
        } catch (e: URISyntaxException) {
            logError("SocketManager", "Invalid socket URL", e)
        }
    }

    /**
     * Disconnects and releases resources.
     */
    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }

    fun isConnected(): Boolean = socket?.connected() ?: false
}
