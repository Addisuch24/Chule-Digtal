package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.chuledigita.mobile.ui.components.MainScaffold

@Composable
fun DashboardScreen(
    navController: NavHostController,
    onLogout: () -> Unit
) {
    MainScaffold(
        navController = navController,
        title = "Dashboard",
        showBottomBar = false
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text("Dashboard Placeholder")
        }
    }
}
