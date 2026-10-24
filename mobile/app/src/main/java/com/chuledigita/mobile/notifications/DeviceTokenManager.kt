package com.chuledigita.mobile.notifications

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.chuledigita.mobile.data.remote.ApiService
import com.chuledigita.mobile.data.remote.LogoutDeviceRequest
import com.chuledigita.mobile.data.remote.RegisterDeviceRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Manages unique device installation tokens and synchronization with the backend
 * push notification registry, supporting both logged-in and logged-out delivery.
 */
class DeviceTokenManager(
    private val context: Context,
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "DeviceTokenManager"
        private const val PREFS_NAME = "chule_device_prefs"
        private const val KEY_DEVICE_TOKEN = "device_push_token"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Retrieves or generates a persistent device token unique to this app installation.
     */
    fun getOrCreateDeviceToken(): String {
        var token = prefs.getString(KEY_DEVICE_TOKEN, null)
        if (token.isNullOrBlank()) {
            token = "android_${UUID.randomUUID()}"
            prefs.edit().putString(KEY_DEVICE_TOKEN, token).apply()
            Log.d(TAG, "Generated new persistent device push token: $token")
        }
        return token
    }

    /**
     * Registers the device token with the backend.
     * Can be invoked whether the user is logged in or logged out.
     */
    fun registerDevice(userId: Long? = null, receiveLoggedOut: Boolean = true) {
        val token = getOrCreateDeviceToken()
        scope.launch {
            try {
                val request = RegisterDeviceRequest(
                    deviceToken = token,
                    userId = userId,
                    platform = "android",
                    receiveLoggedOut = receiveLoggedOut
                )
                val response = apiService.registerDevice(request)
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully registered device push token with backend (userId=$userId)")
                } else {
                    Log.w(TAG, "Device registration returned non-success HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register device push token: ${e.message}")
            }
        }
    }

    /**
     * Disassociates user account on logout while retaining logged-out push notification delivery.
     */
    fun logoutDevice() {
        val token = getOrCreateDeviceToken()
        scope.launch {
            try {
                val response = apiService.logoutDevice(LogoutDeviceRequest(deviceToken = token))
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully disassociated user account from device push token")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to disassociate device on logout: ${e.message}")
            }
        }
    }
}
