package com.chuledigita.mobile.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility for formatting backend ISO timestamps into human-readable date and time strings.
 */
object DateTimeUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm A", Locale.getDefault())

    fun formatDate(isoString: String?): String {
        if (isoString.isNullOrBlank()) return ""
        return try {
            val date = isoFormat.parse(isoString)
            if (date != null) dateFormat.format(date) else ""
        } catch (e: Exception) {
            isoString.split("T").firstOrNull() ?: ""
        }
    }

    fun formatTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return ""
        return try {
            val date = isoFormat.parse(isoString)
            if (date != null) timeFormat.format(date) else ""
        } catch (e: Exception) {
            ""
        }
    }
}
