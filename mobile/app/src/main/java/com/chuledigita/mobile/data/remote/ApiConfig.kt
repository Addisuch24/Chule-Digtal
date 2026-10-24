package com.chuledigita.mobile.data.remote

/**
 * Central API configuration for Chule Digital backend connection.
 * 
 * Default setup supports both Android Emulator (10.0.2.2) and physical Android devices
 * on the local Wi-Fi network (10.42.0.83 / 192.168.137.75).
 */
object ApiConfig {
    /** Default base URL for Android Emulator accessing host localhost on port 5000 */
    const val EMULATOR_BASE_URL = "http://10.0.2.2:5000/api/"

    /** LAN IP address for testing on physical Android devices over local Wi-Fi / Hotspot */
    const val LAN_BASE_URL = "http://192.168.137.176:5000/api/"

    /** Centralized base URL used across the mobile application */
    var BASE_URL: String = LAN_BASE_URL

    /** Connection timeouts in seconds */
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L

    /**
     * Initializes BASE_URL from SharedPreferences if previously configured by user.
     */
    fun init(context: android.content.Context) {
        val prefs = context.getSharedPreferences("chule_network_prefs", android.content.Context.MODE_PRIVATE)
        val saved = prefs.getString("custom_base_url", null)
        if (!saved.isNullOrBlank() && !saved.contains("10.232.84.204") && !saved.contains("192.168.188.3")) {
            BASE_URL = saved
        } else {
            BASE_URL = LAN_BASE_URL
        }
    }

    /**
     * Updates BASE_URL at runtime and persists it for subsequent app launches.
     */
    fun updateBaseUrl(context: android.content.Context, newUrl: String) {
        val trimmed = newUrl.trim()
        val normalized = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        BASE_URL = normalized
        context.getSharedPreferences("chule_network_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("custom_base_url", normalized)
            .apply()
    }
}
