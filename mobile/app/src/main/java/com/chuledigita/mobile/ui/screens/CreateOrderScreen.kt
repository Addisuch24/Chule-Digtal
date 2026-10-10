package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.ShoppingCart
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
import com.chuledigita.mobile.domain.model.Order
import com.chuledigita.mobile.domain.model.OrderStatus
import com.chuledigita.mobile.domain.model.VerificationStatus
import com.chuledigita.mobile.ui.components.MainScaffold
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CreateOrderScreen(
    navController: NavHostController
) {
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    var customerError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }

    val customers = MockDataProvider.customers
    val scrollState = rememberScrollState()

    MainScaffold(
        navController = navController,
        title = "New Order",
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
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Register Sale", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Log a new customer purchase", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    },
                    isError = customerError != null,
                    supportingText = {
                        if (customerError != null) Text(customerError!!)
                    },
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
                // Invisible box to handle click on the whole field
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { expanded = true }
                )
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    if (input.isEmpty() || input.toDoubleOrNull() != null) {
                        amount = input
                        amountError = null
                    }
                },
                label = { Text("Total Amount (ETB) *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = amountError != null,
                supportingText = {
                    if (amountError != null) Text(amountError!!)
                },
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { 
                    description = it
                    descriptionError = null
                },
                label = { Text("Order Details *") },
                placeholder = { Text("e.g. 5x Cement Bags, 2x Paint Cans") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                isError = descriptionError != null,
                supportingText = {
                    if (descriptionError != null) Text(descriptionError!!)
                },
                shape = RoundedCornerShape(12.dp)
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        Icons.Default.Info, 
                        contentDescription = null, 
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Orders are marked as PENDING until verified by an administrator. The customer's balance will be adjusted upon confirmation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (selectedCustomer == null) customerError = "Please select a customer"
                    if (amt == null || amt <= 0) amountError = "Enter a valid positive amount"
                    if (description.isBlank()) descriptionError = "Please list the items or provide details"
                    
                    if (customerError == null && amountError == null && descriptionError == null) {
                        val newOrder = Order(
                            id = UUID.randomUUID().toString(),
                            orderNumber = "ORD-" + System.currentTimeMillis().toString().takeLast(6),
                            customerId = selectedCustomer!!.id,
                            customerName = selectedCustomer!!.name,
                            description = description,
                            orderDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date()),
                            totalAmount = amt!!,
                            status = OrderStatus.PENDING,
                            verificationStatus = VerificationStatus.PENDING,
                            itemsCount = 1,
                            takenByFamilyMember = MockDataProvider.currentFamilyMember.name,
                            isVerified = false
                        )
                        MockDataProvider.addOrder(newOrder)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Create Order", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
