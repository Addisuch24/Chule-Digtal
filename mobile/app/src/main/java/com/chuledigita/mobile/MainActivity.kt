package com.chuledigita.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chuledigita.mobile.ui.navigation.AppNavigation
import com.chuledigita.mobile.ui.theme.ChuleDigitalTheme

/**
 * Main activity serving as entry point for Chule Digital Jetpack Compose UI.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChuleDigitalTheme {
                AppNavigation()
            }
        }
    }
}
