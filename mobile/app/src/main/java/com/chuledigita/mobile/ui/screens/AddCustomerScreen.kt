package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chuledigita.mobile.data.mock.MockDataProvider
import com.chuledigita.mobile.ui.components.ConfirmDialog
import com.chuledigita.mobile.ui.components.MainScaffold

@Composable
fun AddCustomerScreen(
    navController: NavHostController,
    viewModel: CustomersViewModel = viewModel(factory = CustomersViewModel.Factory)
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    
    var nameError by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()

    MainScaffold(
        navController = navController,
        title = "Add Customer",
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "New Customer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Register a new client in the system", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            if (nameError) {
                Text("Name is required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Detail Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        MockDataProvider.addCustomer(name, phone.ifBlank { null }, note.ifBlank { null })
                        viewModel.refresh()
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Customer")
            }
            
            OutlinedButton(
                onClick = { 
                    if (name.isNotEmpty() || phone.isNotEmpty() || note.isNotEmpty()) {
                        showCancelDialog = true
                    } else {
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Cancel")
            }
        }

        if (showCancelDialog) {
            ConfirmDialog(
                title = "Discard Changes?",
                message = "Are you sure you want to discard this new customer entry?",
                onConfirm = { 
                    showCancelDialog = false
                    navController.popBackStack() 
                },
                onDismiss = { showCancelDialog = false }
            )
        }
    }
}
