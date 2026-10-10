package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.chuledigita.mobile.data.mock.MockDataProvider
import com.chuledigita.mobile.domain.model.*
import com.chuledigita.mobile.ui.components.*
import com.chuledigita.mobile.ui.theme.SuccessColor
import com.chuledigita.mobile.ui.theme.ErrorColor
import com.chuledigita.mobile.ui.theme.WarningColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialMovementDetailsScreen(
    movementId: String,
    navController: NavHostController
) {
    var movement by remember { mutableStateOf(MockDataProvider.financialMovements.find { it.id == movementId }) }
    var showVerificationDialog by remember { mutableStateOf(false) }
    var isRejecting by remember { mutableStateOf(false) }

    MainScaffold(
        navController = navController,
        title = "Transaction Details",
        showBottomBar = false
    ) { paddingValues ->
        if (movement == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Transaction not found.", style = MaterialTheme.typography.titleLarge)
            }
        } else {
            val currentMov = movement!!
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Amount Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentMov.type == MovementType.DEBIT) ErrorColor.copy(alpha = 0.08f) 
                                            else SuccessColor.copy(alpha = 0.08f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val isDebit = currentMov.type == MovementType.DEBIT
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDebit) ErrorColor else SuccessColor,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = if (isDebit) "DEBIT (ADD DEBT)" else "CREDIT (REDUCTION)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "${if (isDebit) "+" else "-"}${currentMov.amount.toInt()} ETB",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Black,
                                color = if (isDebit) ErrorColor else SuccessColor
                            )
                            Text(
                                text = if (isDebit) "Increasing customer balance" else "Decreasing customer balance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Status Message
                    when (currentMov.verificationStatus) {
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
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = SuccessColor)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Verified Transaction: Successfully applied to balance.", style = MaterialTheme.typography.bodyMedium, color = SuccessColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        VerificationStatus.REJECTED -> {
                            Surface(
                                color = ErrorColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorColor.copy(alpha = 0.3f))
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = ErrorColor)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Rejected Transaction: Marked as invalid.", style = MaterialTheme.typography.bodyMedium, color = ErrorColor, fontWeight = FontWeight.Bold)
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
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SectionLabel(title = "Transaction Information")
                            
                            InfoRow(label = "Customer", value = currentMov.customerName, icon = Icons.Outlined.Person)
                            InfoRow(label = "Date recorded", value = currentMov.date, icon = Icons.Outlined.CalendarToday)
                            InfoRow(label = "Recorded by", value = currentMov.recordedBy, icon = Icons.Outlined.Badge)
                            if (currentMov.paymentMethod != null) {
                                InfoRow(label = "Payment Method", value = currentMov.paymentMethod.name.replace("_", " "), icon = Icons.Outlined.Payments)
                            }
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            
                            SectionLabel(title = "Description / Reference")
                            Text(text = currentMov.description, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    // Verification Actions
                    if (currentMov.verificationStatus == VerificationStatus.PENDING) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { 
                                    isRejecting = false
                                    showVerificationDialog = true 
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
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
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorColor),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorColor.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Reject Transaction")
                            }
                        }
                    } else if (currentMov.verificationNote != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(text = "Admin Verification Note", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = currentMov.verificationNote ?: "", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }

        if (showVerificationDialog && movement != null) {
            var note by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showVerificationDialog = false },
                title = { Text(if (isRejecting) "Reject Transaction" else "Verify Transaction", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (isRejecting) "Are you sure you want to mark this as invalid? No balance change will occur."
                                   else "Confirming this will update the customer's official balance immediately.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            placeholder = { Text("Add an optional note...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newStatus = if (isRejecting) VerificationStatus.REJECTED else VerificationStatus.VERIFIED
                            val defaultNote = if (isRejecting) "Rejected by admin" else "Verified by admin"
                            MockDataProvider.updateVerificationStatus(movement!!.id, newStatus, note.ifBlank { defaultNote })
                            movement = movement?.copy(
                                verificationStatus = newStatus,
                                verificationNote = note.ifBlank { defaultNote }
                            )
                            showVerificationDialog = false
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
}

@Composable
private fun SectionLabel(title: String) {
    Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}

@Composable
private fun InfoRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}
