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

/**
 * Real-time Notifications screen connected to the verified backend API and Socket.IO.
 * Displays business activities performed by other family members.
 */
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
        onLogout = onLogout
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Action Header
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { viewModel.refresh() },
                        enabled = !isRefreshing,
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("አድስ", style = MaterialTheme.typography.labelLarge)
                    }

                    // Backend limitation: Mark all as read is not implemented.
                    // We only show mark as read for individual items or could implement locally.
                }
            }

            when (val state = uiState) {
                is NotificationsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                    }
                }
                is NotificationsUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Surface(
                                modifier = Modifier.size(80.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.NotificationsNone, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(text = "ምንም ማሳወቂያ የለም", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "አስፈላጊ የንግድ እንቅስቃሴዎች ሲኖሩ እዚህ ይገለጻሉ።",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                is NotificationsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                            Button(onClick = { viewModel.refresh() }, modifier = Modifier.padding(top = 16.dp)) {
                                Text("እንደገና ሞክር")
                            }
                        }
                    }
                }
                is NotificationsUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        items(state.notifications, key = { it.id }) { item ->
                            NotificationRow(
                                item = item,
                                onClick = {
                                    if (!item.isRead) {
                                        viewModel.markAsRead(item.id)
                                    }
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    item: NotificationItem,
    onClick: () -> Unit
) {
    val icon = when (item.type) {
        NotificationType.CUSTOMER_CREATED -> Icons.Outlined.Person
        NotificationType.CUSTOMER_NOTE_CREATED -> Icons.Outlined.NoteAdd
        NotificationType.BUSINESS_NOTE_CREATED -> Icons.Outlined.Notes
        NotificationType.ORDER_CREATED -> Icons.Outlined.ShoppingCart
        NotificationType.ORDER_VERIFIED -> Icons.Outlined.CheckCircle
        NotificationType.ORDER_REJECTED -> Icons.Outlined.ErrorOutline
        NotificationType.ORDER_STATUS_UPDATED -> Icons.Outlined.PublishedWithChanges
        NotificationType.FINANCIAL_MOVEMENT_CREATED -> Icons.Outlined.ReceiptLong
        NotificationType.FINANCIAL_MOVEMENT_VERIFIED -> Icons.Outlined.TaskAlt
        NotificationType.FINANCIAL_MOVEMENT_REJECTED -> Icons.Outlined.Cancel
        NotificationType.PAYMENT_RECORDED -> Icons.Outlined.Payments
        else -> Icons.Outlined.Info
    }

    val iconColor = when (item.type) {
        NotificationType.ORDER_REJECTED, 
        NotificationType.FINANCIAL_MOVEMENT_REJECTED -> MaterialTheme.colorScheme.error
        
        NotificationType.ORDER_VERIFIED, 
        NotificationType.FINANCIAL_MOVEMENT_VERIFIED,
        NotificationType.PAYMENT_RECORDED -> SuccessColor
        
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        onClick = onClick,
        color = if (item.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (item.isRead) FontWeight.SemiBold else FontWeight.ExtraBold,
                        color = if (item.isRead) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                    )
                    if (!item.isRead) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {}
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📅 ${item.date}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "  🕒 ${item.time}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (item.actor != null) {
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = item.actor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

val SuccessColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
