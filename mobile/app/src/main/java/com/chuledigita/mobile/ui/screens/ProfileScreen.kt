package com.chuledigita.mobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.chuledigita.mobile.domain.model.UserSession
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.components.ProfileHeaderWave
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
        title = "Profile",
        onLogout = { viewModel.logout(onLogout) },
        showBottomBar = true
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OffWhiteBg)
                .padding(paddingValues)
        ) {
            when (uiState) {
                is ProfileUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldGreen)
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
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(12.dp)
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
    var showAccountDetails by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Curved Wave Header with Avatar & Online Badge
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                ProfileHeaderWave(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Portrait Avatar with white border
                    Surface(
                        modifier = Modifier.size(92.dp),
                        shape = CircleShape,
                        color = PureWhiteSurface,
                        border = BorderStroke(3.dp, PureWhiteSurface),
                        shadowElevation = 4.dp
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = LightMint
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(54.dp),
                                    tint = EmeraldGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = session.fullName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Family Member • @${session.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Online Pill Status
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LightMint,
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(6.dp),
                                shape = CircleShape,
                                color = EmeraldGreen
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (session.isActive) "Online" else "Offline",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        }
                    }
                }
            }
        }

        // Action Options List (Matching reference board)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileOptionCard(
                    title = "Account Information",
                    subtitle = "View and update your details",
                    icon = Icons.Outlined.Person,
                    onClick = { showAccountDetails = !showAccountDetails }
                )

                AnimatedVisibility(visible = showAccountDetails) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = PureWhiteSurface,
                        border = BorderStroke(1.dp, BorderDivider),
                        shadowElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "የመለያ መረጃ (Account Details)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy
                            )

                            ProfileInfoItem(
                                label = "Name / ስም",
                                value = session.fullName,
                                icon = Icons.Outlined.Person
                            )

                            ProfileInfoItem(
                                label = "Phone / ስልክ",
                                value = session.phone.ifBlank { "Not provided (አልተገለጸም)" },
                                icon = Icons.Outlined.Phone
                            )

                            ProfileInfoItem(
                                label = "Google Account / ኢሜይል",
                                value = session.email ?: "Not provided (አልተገለጸም)",
                                icon = Icons.Outlined.Email
                            )

                            ProfileInfoItem(
                                label = "Username / የተጠቃሚ ስም",
                                value = session.username,
                                icon = Icons.Outlined.Badge
                            )

                            ProfileInfoItem(
                                label = "Status / ሁኔታ",
                                value = if (session.isActive) "ንቁ አባል (Active Member)" else "Inactive",
                                icon = Icons.Outlined.CheckCircle
                            )
                        }
                    }
                }

                ProfileOptionCard(
                    title = "Security",
                    subtitle = "Change password and manage security",
                    icon = Icons.Outlined.Shield,
                    onClick = { showAccountDetails = true }
                )

                ProfileOptionCard(
                    title = "Preferences",
                    subtitle = "App settings and notifications",
                    icon = Icons.Outlined.Settings,
                    onClick = { navController.navigate(Screen.Settings.route) }
                )

                ProfileOptionCard(
                    title = "Help & Support",
                    subtitle = "Get help and contact support",
                    icon = Icons.Outlined.HelpOutline,
                    onClick = { /* Help & support info */ }
                )
            }
        }

        // Red Outlined Logout Button (Matching reference board)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ErrorColor
                    ),
                    border = BorderStroke(1.2.dp, ErrorColor.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Outlined.Logout,
                        contentDescription = null,
                        tint = ErrorColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Logout",
                        fontWeight = FontWeight.Bold,
                        color = ErrorColor,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PureWhiteSurface,
        border = BorderStroke(1.dp, BorderDivider),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = LightMint,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = EmeraldGreen
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ProfileInfoItem(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = EmeraldGreen
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}

