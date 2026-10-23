package com.chuledigita.mobile.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.chuledigita.mobile.MainActivity
import com.chuledigita.mobile.R
import com.chuledigita.mobile.domain.model.NotificationItem
import com.chuledigita.mobile.domain.model.NotificationType
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Helper to display Android system notifications when the application is in the background.
 */
object SystemNotificationHelper {

    private const val TAG = "SystemNotificationHelper"
    const val CHANNEL_ID = "chule_digital_updates_channel"
    private const val CHANNEL_NAME = "Chule Digital Updates"
    private const val CHANNEL_DESCRIPTION = "Real-time updates for orders, notes, finance, and customers."

    val isAppInForeground = AtomicBoolean(false)

    /**
     * Initializes the notification channel on Android 8.0+ (API 26+).
     */
    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Posts a system status bar notification if the app is currently in the background
     * and system notification permissions are granted.
     */
    fun postSystemNotificationIfBackground(context: Context, item: NotificationItem) {
        if (isAppInForeground.get()) {
            // Foreground in-app pop-up takes precedence
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "POST_NOTIFICATIONS permission not granted; skipping system notification")
                return
            }
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("NOTIFICATION_ID", item.id)
                putExtra("NOTIFICATION_TYPE", item.type.name)
                putExtra("ENTITY_TYPE", item.entityType)
                putExtra("ENTITY_ID", item.entityId)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                item.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val smallIconRes = R.mipmap.ic_launcher

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallIconRes)
                .setContentTitle(item.title)
                .setContentText(item.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(item.message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (item.id.toLongOrNull() ?: System.currentTimeMillis()).toInt()
            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Error posting system notification: ${e.message}", e)
        }
    }
}
