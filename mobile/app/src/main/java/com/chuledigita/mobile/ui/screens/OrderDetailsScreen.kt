package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.chuledigita.mobile.domain.model.Order
import com.chuledigita.mobile.domain.model.OrderStatus
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.components.OrderStatusBadge
import com.chuledigita.mobile.ui.theme.SuccessColor
import com.chuledigita.mobile.ui.theme.WarningColor
import kotlinx.coroutines.delay

/**
 * Detailed view of a single order.
 * Shows order info, creator, and completion actor if applicable.
 */
@Composable
fun OrderDetailsScreen(
    orderId: String,
    navController: NavHostController,
    viewModel: OrderDetailsViewModel = viewModel(factory = OrderDetailsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(orderId) {
        viewModel.loadOrder(orderId)
    }

    MainScaffold(
        navController = navController,
        title = "የትዕዛዝ ዝርዝር መረጃ",
        showBottomBar = false
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is OrderDetailsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is OrderDetailsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "የትዕዛዝ ዝርዝር ማምጣት አልተቻለም።", color = MaterialTheme.colorScheme.error)
                            Button(onClick = { viewModel.loadOrder(orderId) }, modifier = Modifier.padding(top = 16.dp)) {
                                Text("እንደገና ሞክር")
                            }
                        }
                    }
                }
                is OrderDetailsUiState.Success -> {
                    OrderDetailsContent(
                        order = state.order,
                        successMessage = state.successMessage,
                        onClearMessage = { viewModel.clearMessage() },
                        onCompleteClick = { viewModel.updateStatus(orderId, OrderStatus.COMPLETED) }
                    )
                }
            }
        }
    }
}

@Composable
fun OrderDetailsContent(
    order: Order,
    successMessage: String?,
    onClearMessage: () -> Unit,
    onCompleteClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Success Feedback Banner
        successMessage?.let { msg ->
            Surface(
                color = SuccessColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SuccessColor.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessColor)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = msg, style = MaterialTheme.typography.bodyMedium, color = SuccessColor, fontWeight = FontWeight.Bold)
                }
            }
            LaunchedEffect(msg) {
                delay(5000)
                onClearMessage()
            }
        }

        // Title & Status Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = order.orderNumber,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "መለያ ቁጥር: #${order.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            OrderStatusBadge(status = order.status)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Order Information
        DetailSectionView(
            label = "የትዕዛዝ ዝርዝር", 
            value = order.description, 
            icon = Icons.Outlined.Description
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Creator information Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "የመዘገበው",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = order.takenByFamilyMember,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = order.orderDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = order.orderTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        // Completion Section Area
        if (order.status == OrderStatus.COMPLETED) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SuccessColor.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, SuccessColor.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "የተጠናቀቀውን ተግባር ያከናወነው",
                        style = MaterialTheme.typography.labelMedium,
                        color = SuccessColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp), tint = SuccessColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = order.completionActor ?: "-",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = order.completionDate ?: "-", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = order.completionTime ?: "-", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        } else if (order.status != OrderStatus.CANCELLED) {
            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessColor)
            ) {
                Text("እንደተጠናቀቀ አረጋግጥ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Confirmation Dialog
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("ትዕዛዝ ማጠናቀቂያ ማረጋገጫ", fontWeight = FontWeight.Bold) },
                text = { Text("ትዕዛዙን እንደተጠናቀቀ ማረጋገጥ ይፈልጋሉ?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmDialog = false
                            onCompleteClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessColor)
                    ) {
                        Text("አረጋግጥ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("ሰርዝ")
                    }
                }
            )
        }
    }
}

@Composable
private fun DetailSectionView(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
            )
        }
    }
}
