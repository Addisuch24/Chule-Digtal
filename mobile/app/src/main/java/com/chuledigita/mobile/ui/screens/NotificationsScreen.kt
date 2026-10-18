package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.chuledigita.mobile.domain.model.NotificationItem
import com.chuledigita.mobile.domain.model.NotificationType
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavHostController,
    onLogout: () -> Unit,
    viewModel: NotificationsViewModel = viewModel(factory = NotificationsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    MainScaffold(
        navController = navController,
        title = "ማሳወቂያዎች",
        onLogout = onLogout,
        showBottomBar = true
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmOffWhite)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    NotificationsHeader(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() }
                    )
                }

                when (val state = uiState) {
                    is NotificationsUiState.Loading -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = BrandGreen, strokeWidth = 3.dp)
                            }
                        }
                    }
                    is NotificationsUiState.Empty -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        modifier = Modifier.size(64.dp),
                                        shape = CircleShape,
                                        color = BrandGreen.copy(alpha = 0.1f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Outlined.NotificationsNone,
                                                contentDescription = null,
                                                modifier = Modifier.size(32.dp),
                                                tint = BrandGreen
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(text = "ምንም ማሳወቂያ የለም", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrandGreen)
                                    Text(
                                        text = "አስፈላጊ የንግድ እንቅስቃሴዎች ሲኖሩ እዚህ ይገለጻሉ።",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BrandGreen.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    is NotificationsUiState.Error -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(text = state.message, color = ErrorColor, textAlign = TextAlign.Center)
                                    Button(
                                        onClick = { viewModel.refresh() },
                                        modifier = Modifier.padding(top = 16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                                    ) {
                                        Text("እንደገና ሞክር")
                                    }
                                }
                            }
                        }
                    }
                    is NotificationsUiState.Success -> {
                        items(state.notifications, key = { it.id }) { item ->
                            ProfessionalNotificationCard(
                                item = item,
                                onClick = {
                                    if (!item.isRead) {
                                        viewModel.markAsRead(item.id)
                                    }
                                }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun NotificationsHeader(
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "ALERTS & ACTIVITY",
                style = MaterialTheme.typography.labelSmall,
                color = BrandGold,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "ማሳወቂያዎች",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = BrandGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "የንግድ እንቅስቃሴዎች እና ማሳወቂያዎች ማዕከል",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandGreen.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onRefresh,
            enabled = !isRefreshing,
            modifier = Modifier
                .background(CreamSurface, CircleShape)
                .border(1.dp, BrandGreen.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = BrandGreen, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ProfessionalNotificationCard(
    item: NotificationItem,
    onClick: () -> Unit
) {
    val icon = when (item.type) {
        NotificationType.CUSTOMER_CREATED -> Icons.Outlined.PersonAdd
        NotificationType.FINANCIAL_MOVEMENT_CREATED,
        NotificationType.FINANCIAL_MOVEMENT_VERIFIED,
        NotificationType.PAYMENT_RECORDED -> Icons.Outlined.ReceiptLong
        else -> Icons.Outlined.Notifications
    }

    val iconColor = when (item.type) {
        NotificationType.CUSTOMER_CREATED -> SuccessColor
        else -> BrandBlue
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (item.isRead) CreamSurface else Color.White,
        border = BorderStroke(
            width = if (item.isRead) 1.dp else 1.5.dp,
            color = if (item.isRead) BrandGreen.copy(alpha = 0.08f) else BrandGreen.copy(alpha = 0.25f)
        ),
        shadowElevation = if (item.isRead) 1.dp else 3.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (item.isRead) FontWeight.SemiBold else FontWeight.Bold,
                        color = BrandGreen
                    )
                    if (!item.isRead) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = CircleShape,
                            color = BrandGold
                        ) {}
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandGreen.copy(alpha = 0.75f),
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = BrandGreen.copy(alpha = 0.4f), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${item.date} • ${item.time}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandGreen.copy(alpha = 0.45f)
                        )
                    }
                    if (item.actor != null) {
                        Surface(
                            color = BrandGreen.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.actor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
