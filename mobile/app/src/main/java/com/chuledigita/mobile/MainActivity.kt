package com.chuledigita.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.chuledigita.mobile.notifications.SystemNotificationHelper
import com.chuledigita.mobile.ui.navigation.AppNavigation
import com.chuledigita.mobile.ui.theme.ChuleDigitalTheme

/**
 * Main activity serving as entry point for Chule Digital Jetpack Compose UI.
 */
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Gracefully handled; if granted, background notifications will be posted
    }

    val intentFlow = kotlinx.coroutines.flow.MutableSharedFlow<android.content.Intent>(
        replay = 1,
        extraBufferCapacity = 5
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize notification channel for background updates
        SystemNotificationHelper.initNotificationChannel(this)

        // 2. Request POST_NOTIFICATIONS permission on Android 13+ (API 33+) if not already granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        intent?.let { intentFlow.tryEmit(it) }

        setContent {
            ChuleDigitalTheme {
                AppNavigation(activityIntentFlow = intentFlow)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SystemNotificationHelper.isAppInForeground.set(true)
    }

    override fun onPause() {
        super.onPause()
        SystemNotificationHelper.isAppInForeground.set(false)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentFlow.tryEmit(intent)
    }
}
