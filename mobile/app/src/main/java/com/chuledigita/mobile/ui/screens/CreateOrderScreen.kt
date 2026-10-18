package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.chuledigita.mobile.domain.model.Customer
import com.chuledigita.mobile.ui.components.MainScaffold

@Composable
fun CreateOrderScreen(
    navController: NavHostController,
    viewModel: CreateOrderViewModel = viewModel(factory = CreateOrderViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    var titleError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    MainScaffold(
        navController = navController,
        title = "አዲስ ትዕዛዝ (New Order)",
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
                        Icon(
                            Icons.Outlined.ShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "ትዕዛዝ መዝግብ (Register Order)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "አዲስ የደንበኛ ትዕዛዝ ወደ ሲስተሙ ያስገቡ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Backend Server Error Banner
            uiState.errorMessage?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Optional Customer Selector
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedCustomer?.name ?: "አጠቃላይ ደንበኛ (Optional Customer)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("ደንበኛ (Customer)") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    DropdownMenuItem(
                        text = { Text("አጠቃላይ ደንበኛ (General / None)") },
                        onClick = {
                            selectedCustomer = null
                            expanded = false
                        }
                    )
                    uiState.customers.forEach { customer ->
                        DropdownMenuItem(
                            text = { Text(customer.name) },
                            onClick = {
                                selectedCustomer = customer
                                expanded = false
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

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { input ->
                    title = input
                    titleError = if (input.length > 150) "የትዕዛዝ ርዕስ ከ150 ፊደላት መብለጥ የለበትም።" else null
                    viewModel.clearError()
                },
                label = { Text("የትዕዛዝ ርዕስ (Order Title) *") },
                placeholder = { Text("ለምሳሌ፦ የስንዴ ዱቄት ትዕዛዝ") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = titleError != null,
                supportingText = {
                    if (titleError != null) Text(titleError!!)
                },
                shape = RoundedCornerShape(12.dp)
            )

            // Details / Description Field
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    descriptionError = null
                    viewModel.clearError()
                },
                label = { Text("የትዕዛዝ ዝርዝር (Order Details) *") },
                placeholder = { Text("ለምሳሌ፦ 24 ደብተሮች እና ወደ ቦሌ ማድረስ") },
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
                        text = "አዲስ ትዕዛዝ ሲመዘገብ ወደ ሲስተሙ በPENDING ሁኔታ የሚገባ ሲሆን በአስተዳዳሪ ከተረጋገጠ በኋላ ይፈጸማል።",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (title.isBlank()) titleError = "እባክዎ የትዕዛዝ ርዕስ ያስገቡ"
                    if (description.isBlank()) descriptionError = "እባክዎ የትዕዛዝ ዝርዝር መግለጫ ያስገቡ"

                    if (titleError == null && descriptionError == null && title.isNotBlank() && description.isNotBlank()) {
                        viewModel.submitOrder(
                            title = title,
                            description = description,
                            selectedCustomer = selectedCustomer
                        ) {
                            navController.popBackStack()
                        }
                    }
                },
                enabled = !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "ትዕዛዝ መዝግብ (Create Order)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            OutlinedButton(
                onClick = { navController.popBackStack() },
                enabled = !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("ሰርዝ (Cancel)", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
