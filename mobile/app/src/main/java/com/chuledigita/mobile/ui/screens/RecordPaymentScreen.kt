package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.chuledigita.mobile.data.mock.MockDataProvider
import com.chuledigita.mobile.domain.model.*
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.components.PendingVerificationBanner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentScreen(
    navController: NavHostController,
    initialCustomerId: String? = null
) {
    val customers = MockDataProvider.customers
    var selectedCustomer by remember { 
        mutableStateOf(customers.find { it.id == initialCustomerId } ?: customers.firstOrNull()) 
    }
    var amountText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var description by remember { mutableStateOf("") }
    
    var expandedCustomer by remember { mutableStateOf(false) }
    var expandedMethod by remember { mutableStateOf(false) }

    var customerError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    MainScaffold(
        navController = navController,
        title = "Record Payment",
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
                        Icon(Icons.Outlined.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Collection Form", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Record a new payment from a customer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Customer Selector
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedCustomer?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Customer *") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { expandedCustomer = true }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    },
                    isError = customerError != null,
                    supportingText = { customerError?.let { Text(it) } },
                    shape = RoundedCornerShape(12.dp)
                )
                DropdownMenu(
                    expanded = expandedCustomer,
                    onDismissRequest = { expandedCustomer = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    customers.forEach { customer ->
                        DropdownMenuItem(
                            text = { Text("${customer.name} (${customer.phone})") },
                            onClick = {
                                selectedCustomer = customer
                                expandedCustomer = false
                                customerError = null
                            }
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { expandedCustomer = true }
                )
            }

            // Amount Field
            OutlinedTextField(
                value = amountText,
                onValueChange = { 
                    if (it.isEmpty() || it.toDoubleOrNull() != null) {
                        amountText = it
                        amountError = null
                    }
                },
                label = { Text("Payment Amount (ETB) *") },
                placeholder = { Text("e.g. 5000") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = amountError != null,
                supportingText = { amountError?.let { Text(it) } },
                shape = RoundedCornerShape(12.dp)
            )

            // Payment Method Selector
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = paymentMethod.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payment Method *") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { expandedMethod = true }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                DropdownMenu(
                    expanded = expandedMethod,
                    onDismissRequest = { expandedMethod = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    PaymentMethod.entries.forEach { method ->
                        DropdownMenuItem(
                            text = { Text(method.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                paymentMethod = method
                                expandedMethod = false
                            }
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { expandedMethod = true }
                )
            }

            // Description / Reference Number Field
            OutlinedTextField(
                value = description,
                onValueChange = { 
                    description = it
                    descriptionError = null
                },
                label = { Text("Bank Reference / Details *") },
                placeholder = { Text("e.g. CBE Transfer Ref: TXN99281") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                isError = descriptionError != null,
                supportingText = { descriptionError?.let { Text(it) } },
                shape = RoundedCornerShape(12.dp)
            )

            // Pending Verification Info Banner
            PendingVerificationBanner(
                message = "Note: This payment will be marked as PENDING and requires management verification before impacting the official balance."
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    var valid = true

                    if (selectedCustomer == null) {
                        customerError = "Please select a customer"
                        valid = false
                    }
                    if (amt == null || amt <= 0) {
                        amountError = "Enter a valid positive amount"
                        valid = false
                    }
                    if (description.isBlank()) {
                        descriptionError = "Payment details or reference is required"
                        valid = false
                    }

                    if (valid && selectedCustomer != null && amt != null) {
                        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val newMovement = FinancialMovement(
                            id = "fmov-${System.currentTimeMillis()}",
                            customerId = selectedCustomer!!.id,
                            customerName = selectedCustomer!!.name,
                            type = MovementType.CREDIT,
                            amount = amt,
                            date = currentDate,
                            description = "Payment via ${paymentMethod.name}: $description",
                            recordedBy = MockDataProvider.currentFamilyMember.name,
                            verificationStatus = VerificationStatus.PENDING,
                            paymentMethod = paymentMethod
                        )
                        MockDataProvider.addFinancialMovement(newMovement)
                        showSuccessDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Record Payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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

        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = {
                    showSuccessDialog = false
                    navController.popBackStack()
                },
                icon = {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                },
                title = { Text("Payment Logged", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("The payment of ${amountText} ETB has been recorded successfully.")
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "It is now in the verification queue. You can track its status in the Finance section.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSuccessDialog = false
                            navController.popBackStack()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Finish")
                    }
                }
            )
        }
    }
}
