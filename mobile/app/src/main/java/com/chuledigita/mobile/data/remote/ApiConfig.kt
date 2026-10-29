package com.chuledigita.mobile.data.remote

import com.chuledigita.mobile.BuildConfig

/**
 * Central API configuration for Chule Digital backend connection.
 * Supports development LAN presets as well as secure HTTPS production staging endpoints.
 */
object ApiConfig {
    /** Default base URL for Android Emulator accessing host localhost on port 5000 */
    const val EMULATOR_BASE_URL = "http://10.0.2.2:5000/api/"

    /** LAN IP address for testing on physical Android devices over Mobile Hotspot */
    const val HOTSPOT_BASE_URL = "http://192.168.137.176:5000/api/"

    /** LAN IP address for testing on physical Android devices over Wi-Fi */
    const val WIFI_BASE_URL = "http://192.168.188.3:5000/api/"

    /** Production / Staging API Base URL (enforces HTTPS) */
    val PRODUCTION_BASE_URL: String = BuildConfig.PRODUCTION_API_URL

    /** Default base URL: in debug mode uses local preset, in release mode uses production HTTPS */
    val DEFAULT_BASE_URL: String = PRODUCTION_BASE_URL

    /** Centralized base URL used across the mobile application */
    var BASE_URL: String = DEFAULT_BASE_URL

    /** Connection timeouts in seconds */
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L

    /**
     * Initializes BASE_URL from SharedPreferences if previously configured by user.
     */
    fun init(context: android.content.Context) {
        val prefs = context.getSharedPreferences("chule_network_prefs", android.content.Context.MODE_PRIVATE)
        val saved = prefs.getString("custom_base_url", null)
        if (!saved.isNullOrBlank() && !saved.contains("10.232.84.204")) {
            // In release mode with HTTPS enforced, reject insecure cleartext URLs
            if (BuildConfig.ENFORCE_HTTPS && !saved.startsWith("https://")) {
                BASE_URL = PRODUCTION_BASE_URL
            } else {
                BASE_URL = saved
            }
        } else {
            BASE_URL = DEFAULT_BASE_URL
        }
    }

    /**
     * Updates BASE_URL at runtime and persists it for subsequent app launches.
     */
    fun updateBaseUrl(context: android.content.Context, newUrl: String) {
        val trimmed = newUrl.trim()
        val normalized = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        // Security check: in production release builds, reject non-HTTPS URLs
        if (BuildConfig.ENFORCE_HTTPS && !normalized.startsWith("https://")) {
            BASE_URL = PRODUCTION_BASE_URL
            return
        }
        BASE_URL = normalized
        context.getSharedPreferences("chule_network_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("custom_base_url", normalized)
            .apply()
    }
}
