package com.chuledigita.mobile.notifications

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.chuledigita.mobile.domain.model.NotificationType
import com.chuledigita.mobile.ui.navigation.Screen
import java.util.Collections

/**
 * Lifecycle-safe manager for notification-driven routing, intent de-duplication,
 * and pending navigation across login, session restoration, and cold starts.
 */
class NotificationNavigationManager(
    context: Context? = null,
    customPrefs: SharedPreferences? = null
) {

    companion object {
        private const val TAG = "NotifNavManager"
        private const val PREFS_NAME = "chule_notification_nav_prefs"
        private const val KEY_PENDING_ROUTE = "pending_destination_route"
        private const val KEY_PENDING_NOTIF_ID = "pending_notification_id"
        private const val KEY_PENDING_TIMESTAMP = "pending_timestamp"
        private const val ROUTE_EXPIRATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    private val prefs: SharedPreferences? =
        customPrefs ?: context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // In-memory fallback if SharedPreferences is unavailable (e.g. unit tests)
    private val inMemoryStore = mutableMapOf<String, Any>()

    // In-memory set of handled notification intent IDs/hashes to prevent duplicate navigation on activity recreation
    private val handledIntentKeys: MutableSet<String> = Collections.synchronizedSet(mutableSetOf<String>())

    /**
     * Resolves a safe, validated internal destination route from notification metadata.
     * Arbitrary external URLs or unrestricted commands are strictly rejected.
     */
    fun resolveRoute(
        notificationTypeStr: String?,
        entityType: String?,
        entityId: String?
    ): String {
        val safeEntityId = entityId?.trim()?.takeIf { it.isNotBlank() && it.matches(Regex("^[a-zA-Z0-9_-]+$")) }

        val type = try {
            if (!notificationTypeStr.isNullOrBlank()) {
                NotificationType.valueOf(notificationTypeStr.trim())
            } else {
                NotificationType.SYSTEM
            }
        } catch (e: Exception) {
            NotificationType.SYSTEM
        }

        return when (type) {
            NotificationType.ORDER_CREATED,
            NotificationType.ORDER_VERIFIED,
            NotificationType.ORDER_REJECTED,
            NotificationType.ORDER_STATUS_UPDATED -> {
                if (safeEntityId != null) {
                    Screen.OrderDetails.createRoute(safeEntityId)
                } else {
                    Screen.Orders.route
                }
            }
            NotificationType.BUSINESS_NOTE_CREATED -> {
                if (safeEntityId != null) {
                    Screen.NoteDetails.createRoute(safeEntityId)
                } else {
                    Screen.Notes.route
                }
            }
            NotificationType.CUSTOMER_NOTE_CREATED,
            NotificationType.CUSTOMER_CREATED,
            NotificationType.CUSTOMER_UPDATED -> {
                if (safeEntityId != null) {
                    Screen.CustomerDetails.createRoute(safeEntityId)
                } else {
                    Screen.Customers.route
                }
            }
            NotificationType.CUSTOMER_DELETED,
            NotificationType.CUSTOMER_BULK_DELETED -> {
                Screen.Customers.route
            }
            NotificationType.FINANCIAL_MOVEMENT_CREATED,
            NotificationType.FINANCIAL_MOVEMENT_VERIFIED,
            NotificationType.FINANCIAL_MOVEMENT_REJECTED,
            NotificationType.FINANCIAL_MOVEMENT_DELETED,
            NotificationType.FINANCIAL_MOVEMENT_BULK_DELETED,
            NotificationType.PAYMENT_RECORDED -> {
                if (safeEntityId != null) {
                    Screen.FinancialMovementDetails.createRoute(safeEntityId)
                } else {
                    Screen.Finance.route
                }
            }
            NotificationType.SYSTEM -> {
                Screen.Notifications.route
            }
        }
    }

    /**
     * Checks if this notification intent has already been consumed to prevent duplicate navigation.
     */
    fun isIntentAlreadyHandled(intentKey: String): Boolean {
        return handledIntentKeys.contains(intentKey)
    }

    /**
     * Marks an intent key as handled.
     */
    fun markIntentHandled(intentKey: String) {
        handledIntentKeys.add(intentKey)
    }

    /**
     * Stores a pending navigation destination in persistent storage to survive
     * activity recreation, process restart, or login flow.
     */
    fun setPendingDestination(route: String, notificationId: String? = null) {
        val p = prefs
        if (p != null) {
            p.edit()
                .putString(KEY_PENDING_ROUTE, route)
                .putString(KEY_PENDING_NOTIF_ID, notificationId)
                .putLong(KEY_PENDING_TIMESTAMP, System.currentTimeMillis())
                .apply()
        } else {
            inMemoryStore[KEY_PENDING_ROUTE] = route
            if (notificationId != null) inMemoryStore[KEY_PENDING_NOTIF_ID] = notificationId
            inMemoryStore[KEY_PENDING_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    /**
     * Returns the pending destination route without clearing it.
     */
    fun peekPendingDestination(): String? {
        val p = prefs
        val route: String?
        val timestamp: Long
        if (p != null) {
            route = p.getString(KEY_PENDING_ROUTE, null)
            timestamp = p.getLong(KEY_PENDING_TIMESTAMP, 0L)
        } else {
            route = inMemoryStore[KEY_PENDING_ROUTE] as? String
            timestamp = inMemoryStore[KEY_PENDING_TIMESTAMP] as? Long ?: 0L
        }

        if (route == null) return null
        if (System.currentTimeMillis() - timestamp > ROUTE_EXPIRATION_MS) {
            clearPendingDestination()
            return null
        }
        return route
    }

    /**
     * Consumes and clears the pending destination route upon successful authentication.
     */
    fun consumePendingDestination(): String? {
        val route = peekPendingDestination()
        if (route != null) {
            clearPendingDestination()
        }
        return route
    }

    /**
     * Clears any stored pending navigation destination.
     */
    fun clearPendingDestination() {
        val p = prefs
        if (p != null) {
            p.edit()
                .remove(KEY_PENDING_ROUTE)
                .remove(KEY_PENDING_NOTIF_ID)
                .remove(KEY_PENDING_TIMESTAMP)
                .apply()
        } else {
            inMemoryStore.remove(KEY_PENDING_ROUTE)
            inMemoryStore.remove(KEY_PENDING_NOTIF_ID)
            inMemoryStore.remove(KEY_PENDING_TIMESTAMP)
        }
    }
}
