package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chuledigita.mobile.data.mock.MockDataProvider
import com.chuledigita.mobile.ui.components.ConfirmDialog
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.components.ReadOnlyMessage

@Composable
fun EditCustomerScreen(
    customerId: String,
    navController: NavHostController,
    viewModel: CustomersViewModel = viewModel(factory = CustomersViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val customerFromState = (uiState as? CustomersUiState.Success)?.customers?.find { it.id == customerId }
    val customer = customerFromState ?: remember(customerId) { MockDataProvider.getCustomerById(customerId) }
    
    if (customer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Customer not found")
        }
        return
    }

    val isEditable = true
    
    var name by remember(customer) { mutableStateOf(customer.name) }
    var phone by remember(customer) { mutableStateOf(customer.phone ?: "") }
    var note by remember(customer) { mutableStateOf(customer.detailNote ?: "") }
    
    var nameError by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()

    MainScaffold(
        navController = navController,
        title = "Edit Customer",
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
            if (!isEditable) {
                ReadOnlyMessage()
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Edit Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Modify customer information", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { 
                    name = it
                    nameError = false
                },
                label = { Text("Full Name *") },
                isError = nameError,
                enabled = isEditable,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                enabled = isEditable,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Detail Note") },
                enabled = isEditable,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            if (isEditable) {
                var isUpdating by remember { mutableStateOf(false) }
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            nameError = true
                        } else {
                            isUpdating = true
                            viewModel.updateCustomer(
                                customerId = customer.id,
                                name = name.trim(),
                                phone = phone.trim().ifBlank { null },
                                note = note.trim().ifBlank { null }
                            ) { success, _ ->
                                isUpdating = false
                                if (success) {
                                    navController.popBackStack()
                                }
                            }
                        }
                    },
                    enabled = !isUpdating,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (isUpdating) "Updating..." else "Update Customer")
                }
            }
            
            OutlinedButton(
                onClick = { 
                    if (isEditable && (name != customer.name || phone != (customer.phone ?: "") || note != (customer.detailNote ?: ""))) {
                        showCancelDialog = true
                    } else {
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (isEditable) "Cancel" else "Back")
            }
        }

        if (showCancelDialog) {
            ConfirmDialog(
                title = "Discard Changes?",
                message = "Are you sure you want to discard your changes?",
                onConfirm = { 
                    showCancelDialog = false
                    navController.popBackStack() 
                },
                onDismiss = { showCancelDialog = false }
            )
        }
    }
}
