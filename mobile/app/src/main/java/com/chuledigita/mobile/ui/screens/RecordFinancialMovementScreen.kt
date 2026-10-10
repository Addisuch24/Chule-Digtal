package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.chuledigita.mobile.data.mock.MockDataProvider
import com.chuledigita.mobile.domain.model.Customer
import com.chuledigita.mobile.domain.model.FinancialMovement
import com.chuledigita.mobile.domain.model.MovementType
import com.chuledigita.mobile.domain.model.VerificationStatus
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.components.PendingVerificationBanner
import com.chuledigita.mobile.ui.theme.ErrorColor
import com.chuledigita.mobile.ui.theme.SuccessColor
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordFinancialMovementScreen(
    navController: NavHostController
) {
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var movementType by remember { mutableStateOf(MovementType.DEBIT) }
    var expanded by remember { mutableStateOf(false) }

    var customerError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }

    val customers = MockDataProvider.customers
    val scrollState = rememberScrollState()

    MainScaffold(
        navController = navController,
        title = "Record Movement",
        showBottomBar = false
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Transaction Form", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Adjust customer balance manually", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select Movement Type",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = movementType == MovementType.DEBIT,
                    onClick = { movementType = MovementType.DEBIT },
                    label = { Text("DEBIT (ADD DEBT)") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ErrorColor.copy(alpha = 0.12f),
                        selectedLabelColor = ErrorColor
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = movementType == MovementType.DEBIT,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = ErrorColor
                    )
                )
                FilterChip(
                    selected = movementType == MovementType.CREDIT,
                    onClick = { movementType = MovementType.CREDIT },
                    label = { Text("CREDIT (REDUCE)") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SuccessColor.copy(alpha = 0.12f),
                        selectedLabelColor = SuccessColor
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = movementType == MovementType.CREDIT,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = SuccessColor
                    )
                )
            }

            // Customer Selector
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedCustomer?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Customer *") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    },
                    isError = customerError != null,
                    supportingText = { if (customerError != null) Text(customerError!!) },
                    shape = RoundedCornerShape(12.dp)
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    customers.forEach { customer ->
                        DropdownMenuItem(
                            text = { Text(customer.name) },
                            onClick = {
                                selectedCustomer = customer
                                expanded = false
                                customerError = null
                            }
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { expanded = true }
                )
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { 
                    if (it.isEmpty() || it.toDoubleOrNull() != null) {
                        amount = it
                        amountError = null
                    }
                },
                label = { Text("Amount (ETB) *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = amountError != null,
                supportingText = { if (amountError != null) Text(amountError!!) },
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { 
                    description = it
                    descriptionError = null
                },
                label = { Text("Description / Reason *") },
                placeholder = { Text("e.g. Bulk cement correction, Service charge...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                isError = descriptionError != null,
                supportingText = { if (descriptionError != null) Text(descriptionError!!) },
                shape = RoundedCornerShape(12.dp)
            )

            PendingVerificationBanner(
                message = "This manual adjustment will require admin verification before officially updating the verified balance."
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (selectedCustomer == null) customerError = "Please select a customer"
                    if (amt == null || amt <= 0) amountError = "Enter a valid positive amount"
                    if (description.isBlank()) descriptionError = "Description is required"
                    
                    if (customerError == null && amountError == null && descriptionError == null) {
                        val newMovement = FinancialMovement(
                            id = "fmov-${System.currentTimeMillis()}",
                            customerId = selectedCustomer!!.id,
                            customerName = selectedCustomer!!.name,
                            type = movementType,
                            amount = amt!!,
                            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                            description = description,
                            recordedBy = MockDataProvider.currentFamilyMember.name,
                            verificationStatus = VerificationStatus.PENDING
                        )
                        MockDataProvider.addFinancialMovement(newMovement)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Record Transaction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Cancel", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
