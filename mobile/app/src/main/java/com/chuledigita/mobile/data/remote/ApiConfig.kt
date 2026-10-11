package com.chuledigita.mobile.data.remote

/**
 * Central API configuration for Chule Digital backend connection.
 * 
 * Default setup connects to local Node.js Express server on Android Emulator (10.0.2.2:5000/api/).
 * To test on a physical Android device on the same local Wi-Fi, replace EMULATOR_BASE_URL
 * with your computer's LAN IP address (e.g. "http://192.168.1.50:5000/api/").
 */
object ApiConfig {
    /** Default base URL for Android Emulator accessing host localhost on port 5000 */
    const val EMULATOR_BASE_URL = "http://10.0.2.2:5000/api/"

    /** Centralized base URL used across the mobile application */
    var BASE_URL: String = EMULATOR_BASE_URL

    /** Connection timeouts in seconds */
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L
}
