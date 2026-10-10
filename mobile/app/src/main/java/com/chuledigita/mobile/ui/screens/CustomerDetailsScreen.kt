package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.chuledigita.mobile.data.mock.MockDataProvider
import com.chuledigita.mobile.ui.components.ActionHistoryItem
import com.chuledigita.mobile.ui.components.ConfirmDialog
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.components.ReadOnlyMessage
import com.chuledigita.mobile.ui.navigation.Screen

@Composable
fun CustomerDetailsScreen(
    customerId: String,
    navController: NavHostController,
    viewModel: CustomersViewModel = viewModel(factory = CustomersViewModel.Factory)
) {
    val customer = remember(customerId) { MockDataProvider.getCustomerById(customerId) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (customer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Customer not found")
        }
        return
    }

    val isEditable = customer.createdBy == MockDataProvider.currentFamilyMember.name
    val scrollState = rememberScrollState()

    MainScaffold(
        navController = navController,
        title = "የደንበኛ ዝርዝር",
        showBottomBar = false
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (!isEditable) {
                ReadOnlyMessage(message = "ይህን መረጃ የፈጠረው የቤተሰብ አባል ብቻ ማስተካከል ይችላል።")
            }

            // Profile Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                customer.phone?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Basic Info
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailItem("ማስታወሻ", customer.detailNote ?: "ምንም ማስታወሻ የለም")
                DetailItem("የተመዘገበበት ቀን", customer.registrationDate)
            }

            // Action History Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.History, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "የእንቅስቃሴ ታሪክ", 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                
                if (customer.actionHistory.isEmpty()) {
                    Text(
                        text = "ምንም ታሪክ አልተገኘም", 
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 36.dp)
                    )
                } else {
                    customer.actionHistory.forEach { action ->
                        ActionHistoryItem(action = action)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isEditable) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { navController.navigate(Screen.EditCustomer.createRoute(customer.id)) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("መረጃውን አስተካክል")
                    }
                    
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ደንበኛውን ሰርዝ")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }

        if (showDeleteDialog) {
            ConfirmDialog(
                title = "ደንበኛውን ሰርዝ",
                message = "${customer.name}ን ከዝርዝር ውስጥ መሰረዝ እንደሚፈልጉ እርግጠኛ ነዎት? ይህ ተግባር ሊመለስ አይችልም።",
                onConfirm = {
                    showDeleteDialog = false
                    viewModel.deleteCustomer(customer.id)
                    navController.popBackStack()
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}
