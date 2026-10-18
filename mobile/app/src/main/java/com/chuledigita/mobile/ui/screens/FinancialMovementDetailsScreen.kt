package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
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
import com.chuledigita.mobile.domain.model.*
import com.chuledigita.mobile.ui.components.*
import com.chuledigita.mobile.ui.theme.ErrorColor
import com.chuledigita.mobile.ui.theme.SuccessColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialMovementDetailsScreen(
    movementId: String,
    navController: NavHostController,
    viewModel: FinancialMovementDetailsViewModel = viewModel(factory = FinancialMovementDetailsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(movementId) {
        viewModel.loadMovement(movementId)
    }

    MainScaffold(
        navController = navController,
        title = "Transaction Details",
        showBottomBar = false
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is FinancialMovementDetailsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is FinancialMovementDetailsUiState.Error -> {
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
                            Button(
                                onClick = { viewModel.loadMovement(movementId) },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("እንደገና ሞክር")
                            }
                        }
                    }
                }

                is FinancialMovementDetailsUiState.Success -> {
                    FinancialMovementDetailsContent(
                        movement = state.movement,
                        isSubmitting = state.isSubmitting,
                        successMessage = state.successMessage,
                        errorMessage = state.errorMessage,
                        onClearMessage = { viewModel.clearMessages() },
                        onVerify = { note -> viewModel.verifyMovement(movementId, note) },
                        onReject = { note -> viewModel.rejectMovement(movementId, note) }
                    )
                }
            }
        }
    }
}

@Composable
fun FinancialMovementDetailsContent(
    movement: FinancialMovement,
    isSubmitting: Boolean,
    successMessage: String?,
    errorMessage: String?,
    onClearMessage: () -> Unit,
    onVerify: (String) -> Unit,
    onReject: (String) -> Unit
) {
    var showVerificationDialog by remember { mutableStateOf(false) }
    var isRejecting by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Feedback Banner
            val bannerMsg = successMessage ?: errorMessage
            if (bannerMsg != null) {
                val isSuccess = successMessage != null
                val bannerColor = if (isSuccess) SuccessColor else MaterialTheme.colorScheme.error
                Surface(
                    color = bannerColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, bannerColor.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Default.Check else Icons.Default.ErrorOutline,
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
            }

            // Amount Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (movement.type == MovementType.DEBIT) ErrorColor.copy(alpha = 0.08f)
                    else SuccessColor.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isDebit = movement.type == MovementType.DEBIT
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDebit) ErrorColor else SuccessColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = if (isDebit) "DEBIT (ADD DEBT)" else "CREDIT (REDUCTION / PAYMENT)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "${if (isDebit) "+" else "-"}${movement.amount.toInt()} ETB",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = if (isDebit) ErrorColor else SuccessColor
                    )
                    Text(
                        text = if (isDebit) "Increases customer debt" else "Decreases customer debt / payment",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status Message
            when (movement.verificationStatus) {
                VerificationStatus.PENDING -> {
                    PendingVerificationBanner(
                        message = "Awaiting verification. This amount has not been added to the official balance yet."
                    )
                }

                VerificationStatus.VERIFIED -> {
                    Surface(
                        color = SuccessColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SuccessColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = SuccessColor)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Verified Transaction: Successfully applied to balance.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SuccessColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                VerificationStatus.REJECTED -> {
                    Surface(
                        color = ErrorColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = ErrorColor)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Rejected Transaction: Marked as invalid.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ErrorColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                else -> {}
            }

            // Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionLabel(title = "Transaction Information")

                    InfoRow(label = "Customer", value = movement.customerName, icon = Icons.Outlined.Person)
                    InfoRow(label = "Date recorded", value = movement.date, icon = Icons.Outlined.CalendarToday)
                    InfoRow(label = "Recorded by", value = movement.recordedBy, icon = Icons.Outlined.Badge)
                    if (movement.paymentMethod != null) {
                        InfoRow(
                            label = "Payment Method",
                            value = movement.paymentMethod.name.replace("_", " "),
                            icon = Icons.Outlined.Payments
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    SectionLabel(title = "Description / Reference")
                    Text(text = movement.description, style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Verification Actions
            if (movement.verificationStatus == VerificationStatus.PENDING) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            isRejecting = false
                            showVerificationDialog = true
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessColor)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Verify Transaction", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            isRejecting = true
                            showVerificationDialog = true
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorColor.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Reject Transaction")
                    }
                }
            } else if (!movement.verificationNote.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.5f
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Admin Verification Note",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = movement.verificationNote, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showVerificationDialog) {
        var note by remember { mutableStateOf("") }
        var noteError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showVerificationDialog = false },
            title = {
                Text(
                    if (isRejecting) "Reject Transaction" else "Verify Transaction",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isRejecting) "Are you sure you want to mark this transaction as invalid?"
                        else "Confirming this will officially apply the transaction to customer balance.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = {
                            note = it
                            noteError = null
                        },
                        placeholder = { Text("Add required verification note...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        isError = noteError != null,
                        supportingText = { if (noteError != null) Text(noteError!!) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (note.trim().isBlank()) {
                            noteError = "Note is required for verification/rejection."
                        } else {
                            showVerificationDialog = false
                            if (isRejecting) {
                                onReject(note.trim())
                            } else {
                                onVerify(note.trim())
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRejecting) ErrorColor else SuccessColor
                    )
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVerificationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun InfoRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}
