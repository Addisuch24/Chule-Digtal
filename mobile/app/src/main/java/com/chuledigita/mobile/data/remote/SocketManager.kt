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
    
    private val _notificationsFlow = MutableSharedFlow<JSONObject>(extraBufferCapacity = 10)
    val notificationsFlow: SharedFlow<JSONObject> = _notificationsFlow.asSharedFlow()

    private val _connectionEvents = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val connectionEvents: SharedFlow<String> = _connectionEvents.asSharedFlow()

    companion object {
        const val EVENT_CONNECTED = "connected"
        const val EVENT_DISCONNECTED = "disconnected"
        const val EVENT_RECONNECTING = "reconnecting"
    }

    /**
     * Connects to the socket server using the provided JWT token for authentication.
     */
    fun connect(token: String) {
        if (socket?.connected() == true) return

        try {
            val options = IO.Options.builder()
                .setAuth(mapOf("token" to token))
                .setReconnection(true)
                .setReconnectionAttempts(Int.MAX_VALUE)
                .setReconnectionDelay(1000)
                .setReconnectionDelayMax(5000)
                .build()

            // Remove 'api/' suffix from base URL for socket connection if present
            val socketUrl = baseUrl.removeSuffix("api/").removeSuffix("/")
            socket = IO.socket(socketUrl, options)

            socket?.on(Socket.EVENT_CONNECT) {
                if (BuildConfig.DEBUG) Log.d("SocketManager", "Connected to socket server")
                _connectionEvents.tryEmit(EVENT_CONNECTED)
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                if (BuildConfig.DEBUG) Log.d("SocketManager", "Disconnected from socket server")
                _connectionEvents.tryEmit(EVENT_DISCONNECTED)
            }

            socket?.on("reconnect") {
                if (BuildConfig.DEBUG) Log.d("SocketManager", "Reconnected to socket server")
                _connectionEvents.tryEmit(EVENT_CONNECTED)
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                if (BuildConfig.DEBUG) Log.e("SocketManager", "Connection error: ${args.firstOrNull()}")
            }

            // Real-time notification event from verified backend contract
            socket?.on("notification:new") { args ->
                val data = args.firstOrNull() as? JSONObject
                if (data != null) {
                    if (BuildConfig.DEBUG) Log.d("SocketManager", "Received notification: $data")
                    _notificationsFlow.tryEmit(data)
                }
            }

            socket?.connect()
        } catch (e: URISyntaxException) {
            if (BuildConfig.DEBUG) Log.e("SocketManager", "Invalid socket URL", e)
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
