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
    const val LAN_BASE_URL = "http://10.42.0.83:5000/api/"

    /** Centralized base URL used across the mobile application */
    var BASE_URL: String = LAN_BASE_URL

    /** Connection timeouts in seconds */
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L
}
