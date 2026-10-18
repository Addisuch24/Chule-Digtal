package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.chuledigita.mobile.domain.model.UserSession
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.navigation.Screen
import com.chuledigita.mobile.ui.theme.*

@Composable
fun ProfileScreen(
    navController: NavHostController,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MainScaffold(
        navController = navController,
        title = "መገለጫ (Profile)",
        onLogout = { viewModel.logout(onLogout) },
        showBottomBar = true
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmOffWhite)
                .padding(paddingValues)
        ) {
            when (uiState) {
                is ProfileUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandGreen)
                    }
                }
                is ProfileUiState.Error -> {
                    val errorMsg = (uiState as ProfileUiState.Error).message
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Text(text = errorMsg, color = ErrorColor, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadProfile() },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                            ) {
                                Text("እንደገና ሞክር (Retry)")
                            }
                        }
                    }
                }
                is ProfileUiState.Success -> {
                    val session = (uiState as ProfileUiState.Success).userSession
                    ProfileContent(
                        session = session,
                        navController = navController,
                        onLogout = { viewModel.logout(onLogout) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    session: UserSession,
    navController: NavHostController,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header with Avatar
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(90.dp),
                    shape = CircleShape,
                    color = BrandGreen.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = BrandGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = session.fullName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen
                )

                Text(
                    text = "@${session.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandGreen.copy(alpha = 0.6f)
                )
            }
        }

        // Account Details Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.08f)),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "የመለያ መረጃ (Account Details)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreen
                    )

                    ProfileInfoItem(
                        label = "1. Name / ስም",
                        value = session.fullName,
                        icon = Icons.Outlined.Person
                    )

                    ProfileInfoItem(
                        label = "2. Phone Number / ስልክ",
                        value = session.phone.ifBlank { "Not provided (አልተገለጸም)" },
                        icon = Icons.Outlined.Phone
                    )

                    ProfileInfoItem(
                        label = "3. Google Account / ኢሜይል",
                        value = session.email ?: "Not provided (አልተገለጸም)",
                        icon = Icons.Outlined.Email
                    )

                    ProfileInfoItem(
                        label = "4. Username / የተጠቃሚ ስም",
                        value = session.username,
                        icon = Icons.Outlined.Badge
                    )

                    ProfileInfoItem(
                        label = "Account Status / ሁኔታ",
                        value = if (session.isActive) "ንቁ አባል (Active Member)" else "Inactive",
                        icon = Icons.Outlined.CheckCircle
                    )
                }
            }
        }

        // Settings Navigation
        item {
            Surface(
                onClick = { navController.navigate(Screen.Settings.route) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CreamSurface,
                border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.08f)),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Settings, contentDescription = null, tint = BrandGreen)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "App Settings",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = BrandGreen,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrandGreen.copy(alpha = 0.3f))
                }
            }
        }

        // Logout Action
        item {
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorColor.copy(alpha = 0.1f),
                    contentColor = ErrorColor
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Logout (ውጣ)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileInfoItem(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = BrandGreen.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = BrandGreen.copy(alpha = 0.5f))
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = BrandGreen)
        }
    }
}
