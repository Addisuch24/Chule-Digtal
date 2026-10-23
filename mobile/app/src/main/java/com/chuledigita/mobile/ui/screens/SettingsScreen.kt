package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.theme.*

@Composable
fun SettingsScreen(
    navController: NavHostController,
    onLogout: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    // Local states
    var selectedTheme by remember { mutableStateOf(ThemeManager.selectedTheme) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    MainScaffold(
        navController = navController,
        title = "Settings",
        showBottomBar = false
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(OffWhiteBg)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Appearance Section
            SettingsSection(title = "Appearance", icon = Icons.Outlined.Palette) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhiteSurface),
                    border = BorderStroke(1.dp, BorderDivider),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        listOf("Light Mode", "Dark Mode", "System Default").forEach { theme ->
                            ThemeOptionRow(
                                label = theme,
                                isSelected = selectedTheme == theme,
                                onClick = {
                                    selectedTheme = theme
                                    ThemeManager.selectedTheme = theme
                                }
                            )
                        }
                    }
                }
            }

            // Notifications Section
            SettingsSection(title = "Notifications", icon = Icons.Outlined.NotificationsActive) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhiteSurface),
                    border = BorderStroke(1.dp, BorderDivider),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        NotificationToggleRow(
                            title = "Enable Notifications",
                            subtitle = "Receive alerts for customer registrations, orders, and balance updates",
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Logout
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
                Icon(Icons.Outlined.Logout, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout", fontWeight = FontWeight.Bold, color = ErrorColor, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = EmeraldGreen)
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrandNavy)
        }
        content()
    }
}

@Composable
private fun ThemeOptionRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary) },
        trailingContent = {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = EmeraldGreen)
            )
        },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = TextPrimary) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                    checkedTrackColor = EmeraldGreen,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = BorderDivider
                )
            )
        },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

