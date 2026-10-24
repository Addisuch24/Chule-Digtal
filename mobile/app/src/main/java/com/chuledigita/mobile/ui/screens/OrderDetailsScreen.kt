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
import com.chuledigita.mobile.domain.model.VerificationStatus
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.components.OrderStatusBadge
import com.chuledigita.mobile.ui.theme.SuccessColor
import com.chuledigita.mobile.ui.theme.WarningColor
import kotlinx.coroutines.delay

/**
 * Detailed view of a single order with status, verification, and rejection actions.
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                OutlinedButton(onClick = { navController.navigate(com.chuledigita.mobile.ui.navigation.Screen.Orders.route) }) {
                                    Text("ወደ ትዕዛዞች ዝርዝር")
                                }
                                Button(onClick = { viewModel.loadOrder(orderId) }) {
                                    Text("እንደገና ሞክር")
                                }
                            }
                        }
                    }
                }
                is OrderDetailsUiState.Success -> {
                    OrderDetailsContent(
                        order = state.order,
                        isSubmitting = state.isSubmitting,
                        successMessage = state.successMessage,
                        errorMessage = state.errorMessage,
                        onClearMessage = { viewModel.clearMessage() },
                        onCompleteClick = { viewModel.updateStatus(orderId, OrderStatus.COMPLETED) },
                        onVerifyClick = { note -> viewModel.verifyOrder(orderId, note) },
                        onRejectClick = { note -> viewModel.rejectOrder(orderId, note) }
                    )
                }
            }
        }
    }
}

@Composable
fun OrderDetailsContent(
    order: Order,
    isSubmitting: Boolean,
    successMessage: String?,
    errorMessage: String?,
    onClearMessage: () -> Unit,
    onCompleteClick: () -> Unit,
    onVerifyClick: (String?) -> Unit,
    onRejectClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectionNoteText by remember { mutableStateOf("") }
    var rejectionNoteError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Feedback Banner (Success or Error)
        val bannerMsg = successMessage ?: errorMessage
        if (bannerMsg != null) {
            val isSuccess = successMessage != null
            val bannerColor = if (isSuccess) SuccessColor else MaterialTheme.colorScheme.error
            Surface(
                color = bannerColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = bannerColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = bannerMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = bannerColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            LaunchedEffect(bannerMsg) {
                delay(4000)
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
                    text = order.title ?: order.orderNumber,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "የትዕዛዝ ቁጥር: ${order.orderNumber} • መለያ: #${order.id}",
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

        // Verification Status & Actions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (order.verificationStatus) {
                    VerificationStatus.VERIFIED -> SuccessColor.copy(alpha = 0.08f)
                    VerificationStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    else -> WarningColor.copy(alpha = 0.08f)
                }
            ),
            border = BorderStroke(
                1.dp,
                when (order.verificationStatus) {
                    VerificationStatus.VERIFIED -> SuccessColor.copy(alpha = 0.3f)
                    VerificationStatus.REJECTED -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                    else -> WarningColor.copy(alpha = 0.3f)
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "የማረጋገጫ ሁኔታ (Verification Status)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (order.verificationStatus) {
                            VerificationStatus.VERIFIED -> SuccessColor.copy(alpha = 0.2f)
                            VerificationStatus.REJECTED -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            else -> WarningColor.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = when (order.verificationStatus) {
                                VerificationStatus.VERIFIED -> "የተረጋገጠ (VERIFIED)"
                                VerificationStatus.REJECTED -> "ውድቅ የተደረገ (REJECTED)"
                                else -> "በመጠባበቅ ላይ (PENDING)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (order.verificationStatus) {
                                VerificationStatus.VERIFIED -> SuccessColor
                                VerificationStatus.REJECTED -> MaterialTheme.colorScheme.error
                                else -> WarningColor
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (!order.verificationNote.isNullOrBlank()) {
                    Text(
                        text = "ማስታወሻ: ${order.verificationNote}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (order.verificationStatus == VerificationStatus.PENDING && order.status != OrderStatus.DRAFT) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onVerifyClick(null) },
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessColor),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("አረጋግጥ (Verify)")
                        }
                        OutlinedButton(
                            onClick = {
                                rejectionNoteText = ""
                                rejectionNoteError = null
                                showRejectDialog = true
                            },
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("ውድቅ አድርግ (Reject)")
                        }
                    }
                }
            }
        }

        // Creator Information Section
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
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
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
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = order.orderDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = order.orderTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
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
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = SuccessColor
                        )
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
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = order.completionDate ?: "-",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = order.completionTime ?: "-",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        } else if (order.status != OrderStatus.CANCELLED) {
            Button(
                onClick = { showConfirmDialog = true },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessColor)
            ) {
                Text(
                    "እንደተጠናቀቀ አረጋግጥ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Confirmation Dialog for Marking Complete
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

        // Mandatory Note Dialog for Rejection
        if (showRejectDialog) {
            AlertDialog(
                onDismissRequest = { showRejectDialog = false },
                title = { Text("ትዕዛዝ ውድቅ ማድረጊያ", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("እባክዎ ትዕዛዙ ውድቅ የተደረገበትን ምክንያት ያስገቡ፦")
                        OutlinedTextField(
                            value = rejectionNoteText,
                            onValueChange = {
                                rejectionNoteText = it
                                rejectionNoteError = null
                            },
                            placeholder = { Text("ለምሳሌ፦ እቃዎቹ በክምችት ውስጥ የሉም") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = rejectionNoteError != null,
                            supportingText = {
                                if (rejectionNoteError != null) Text(rejectionNoteError!!)
                            }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (rejectionNoteText.trim().isBlank()) {
                                rejectionNoteError = "ትዕዛዝ ውድቅ ለማድረግ ምክንያቱን መግለጽ ግዴታ ነው።"
                            } else {
                                showRejectDialog = false
                                onRejectClick(rejectionNoteText.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("ውድቅ አድርግ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRejectDialog = false }) {
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
