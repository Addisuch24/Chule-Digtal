package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.chuledigita.mobile.data.mock.MockDataProvider
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.navigation.Screen

@Composable
fun ProfileScreen(
    navController: NavHostController,
    onLogout: () -> Unit
) {
    val familyMember = MockDataProvider.currentFamilyMember
    val recentActivities = MockDataProvider.recentActivities
    
    var showEditMessage by remember { mutableStateOf(false) }

    MainScaffold(
        navController = navController,
        title = "My Profile",
        onLogout = onLogout,
        showBottomBar = true
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(MaterialTheme.colorScheme.background)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header section with Avatar
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                modifier = Modifier.size(100.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccountCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            SmallFloatingActionButton(
                                onClick = { showEditMessage = true },
                                shape = CircleShape,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Avatar", modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = familyMember.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "@${familyMember.name.lowercase().replace(" ", ".")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = familyMember.role.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Account Information
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "Account Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            ProfileInfoRow(label = "Phone", value = "+251 911 22 33 44", icon = Icons.Outlined.Phone)
                            ProfileInfoRow(label = "Email", value = "addisu@chuledigita.com", icon = Icons.Outlined.Email)
                            ProfileInfoRow(label = "Member ID", value = familyMember.id.uppercase(), icon = Icons.Outlined.Badge)
                        }
                    }
                }

                // Navigation & Settings
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileMenuButton(
                            label = "App Settings",
                            icon = Icons.Outlined.Settings,
                            onClick = { navController.navigate(Screen.Settings.route) }
                        )
                        ProfileMenuButton(
                            label = "Notifications",
                            icon = Icons.Outlined.Notifications,
                            onClick = { navController.navigate(Screen.Notifications.route) }
                        )
                        ProfileMenuButton(
                            label = "Edit Profile",
                            icon = Icons.Outlined.Edit,
                            onClick = { showEditMessage = true }
                        )
                    }
                }

                // Logout Action
                item {
                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Outlined.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Log Out from Device", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (showEditMessage) {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = {
                        TextButton(onClick = { showEditMessage = false }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text("Profile editing is disabled in this prototype.")
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileIcon(icon, contentDescription = null, size = 18.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ProfileMenuButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ProfileIcon(icon: ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, color: androidx.compose.ui.graphics.Color) {
    androidx.compose.material3.Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier.size(size),
        tint = color
    )
}
