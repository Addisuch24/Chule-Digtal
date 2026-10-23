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
import com.chuledigita.mobile.ui.components.*
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
    val snackbarHostState = remember { SnackbarHostState() }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    val successState = uiState as? NotificationsUiState.Success
    LaunchedEffect(successState?.actionMessage, successState?.actionError) {
        val msg = successState?.actionMessage ?: successState?.actionError
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessages()
        }
    }

    MainScaffold(
        navController = navController,
        title = "ማሳወቂያዎች",
        onLogout = onLogout,
        showBottomBar = true
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OffWhiteBg)
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeaderBanner(
                    title = "Notifications",
                    subtitle = "Stay updated with the latest activities.",
                    illustration = { NotificationsHeaderIllustration() },
                    action = {
                        IconButton(
                            onClick = { viewModel.refresh() },
                            enabled = !isRefreshing,
                            modifier = Modifier
                                .background(PureWhiteSurface, CircleShape)
                                .border(1.dp, BorderDivider, CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (val state = uiState) {
                        is NotificationsUiState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = EmeraldGreen, strokeWidth = 3.dp)
                                }
                            }
                        }
                        is NotificationsUiState.Empty -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Surface(
                                            modifier = Modifier.size(64.dp),
                                            shape = CircleShape,
                                            color = LightMint
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Outlined.NotificationsNone,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(32.dp),
                                                    tint = EmeraldGreen
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "ምንም ማሳወቂያ የለም",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "አስፈላጊ የንግድ እንቅስቃሴዎች ሲኖሩ እዚህ ይገለጻሉ።",
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                        is NotificationsUiState.Error -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = ErrorColor,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(text = state.message, color = ErrorColor, textAlign = TextAlign.Center)
                                        Button(
                                            onClick = { viewModel.refresh() },
                                            modifier = Modifier.padding(top = 16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("እንደገና ሞክር (Retry)")
                                        }
                                    }
                                }
                            }
                        }
                        is NotificationsUiState.Success -> {
                            item {
                                MultiSelectActionBar(
                                    selectedCount = state.selectedNotificationIds.size,
                                    totalCount = state.notifications.size,
                                    isDeleting = state.isDeletingBatch,
                                    onSelectAll = {
                                        viewModel.selectAllNotifications(state.notifications.map { it.id })
                                    },
                                    onClearSelection = {
                                        viewModel.clearNotificationSelection()
                                    },
                                    onDeleteSelected = {
                                        showBulkDeleteDialog = true
                                    }
                                )
                            }

                            items(state.notifications, key = { it.id }) { item ->
                                ProfessionalNotificationCard(
                                    item = item,
                                    onClick = {
                                        if (state.selectedNotificationIds.isNotEmpty()) {
                                            viewModel.toggleSelectNotification(item.id)
                                        } else if (!item.isRead) {
                                            viewModel.markAsRead(item.id)
                                        }
                                    },
                                    isSelected = state.selectedNotificationIds.contains(item.id),
                                    isSelectionActive = state.selectedNotificationIds.isNotEmpty(),
                                    onToggleSelect = {
                                        viewModel.toggleSelectNotification(item.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }


            if (showBulkDeleteDialog && successState != null) {
                MultiSelectDeleteConfirmationDialog(
                    itemType = "notification",
                    selectedCount = successState.selectedNotificationIds.size,
                    warningMessage = "ማሳወቂያዎች ለእርስዎ መለያ ብቻ የሚሰረዙ ሲሆን የሌሎች ቤተሰብ አባላት ማሳወቂያዎች አይነኩም። (Notifications will be deleted for your account only; other family members' records remain untouched).",
                    isDeleting = successState.isDeletingBatch,
                    onConfirm = {
                        viewModel.bulkDeleteSelectedNotifications { _, _ ->
                            showBulkDeleteDialog = false
                        }
                    },
                    onDismiss = {
                        if (!successState.isDeletingBatch) {
                            showBulkDeleteDialog = false
                        }
                    }
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun ProfessionalNotificationCard(
    item: NotificationItem,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    isSelectionActive: Boolean = false,
    onToggleSelect: (() -> Unit)? = null
) {
    val (icon, iconTint, iconBg) = when (item.type) {
        NotificationType.ORDER_CREATED,
        NotificationType.ORDER_STATUS_UPDATED,
        NotificationType.ORDER_VERIFIED,
        NotificationType.ORDER_REJECTED -> Triple(Icons.Outlined.ShoppingCart, EmeraldGreen, LightMint)

        NotificationType.PAYMENT_RECORDED,
        NotificationType.FINANCIAL_MOVEMENT_CREATED,
        NotificationType.FINANCIAL_MOVEMENT_VERIFIED,
        NotificationType.FINANCIAL_MOVEMENT_REJECTED,
        NotificationType.FINANCIAL_MOVEMENT_DELETED,
        NotificationType.FINANCIAL_MOVEMENT_BULK_DELETED -> Triple(Icons.Outlined.ReceiptLong, BrandTeal, BrandTeal.copy(alpha = 0.12f))

        NotificationType.BUSINESS_NOTE_CREATED,
        NotificationType.CUSTOMER_NOTE_CREATED -> Triple(Icons.Outlined.EditNote, WarningColor, WarningColor.copy(alpha = 0.15f))

        NotificationType.CUSTOMER_CREATED,
        NotificationType.CUSTOMER_UPDATED,
        NotificationType.CUSTOMER_DELETED,
        NotificationType.CUSTOMER_BULK_DELETED -> Triple(Icons.Outlined.Person, BrandNavy, BrandNavy.copy(alpha = 0.10f))

        else -> Triple(Icons.Outlined.Notifications, EmeraldGreen, LightMint)
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) LightMint.copy(alpha = 0.45f) else PureWhiteSurface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) EmeraldGreen else BorderDivider
        ),
        shadowElevation = if (isSelected) 2.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (onToggleSelect != null) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = EmeraldGreen,
                        uncheckedColor = TextSecondary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            Surface(
                shape = CircleShape,
                color = iconBg,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

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
                        color = TextPrimary
                    )
                    if (!item.isRead) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = CircleShape,
                            color = EmeraldGreen
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${item.date} • ${item.time}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    if (item.actor != null) {
                        Surface(
                            color = LightMint,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.actor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

