package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.chuledigita.mobile.domain.model.CustomerNote
import com.chuledigita.mobile.ui.components.ConfirmDialog
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.navigation.Screen
import com.chuledigita.mobile.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CustomerDetailsScreen(
    customerId: String,
    navController: NavHostController,
    viewModel: CustomerDetailsViewModel = viewModel(factory = CustomerDetailsViewModel.provideFactory(customerId))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var newNoteText by remember { mutableStateOf("") }
    var isSavingNote by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeletingCustomer by remember { mutableStateOf(false) }

    MainScaffold(
        navController = navController,
        title = "የደንበኛ ዝርዝር (Customer)",
        showBottomBar = false
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OffWhiteBg)
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is CustomerDetailsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldGreen)
                    }
                }

                is CustomerDetailsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = ErrorColor,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadCustomerDetails() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("እንደገና ሞክር (Retry)")
                        }
                    }
                }

                is CustomerDetailsUiState.Success -> {
                    val customer = state.customer
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Profile Header Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = PureWhiteSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
                            shadowElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = LightMint,
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldGreen.copy(alpha = 0.3f)),
                                    modifier = Modifier.size(76.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp),
                                            tint = EmeraldGreen
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = customer.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                customer.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = phone,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        // Basic Info Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = PureWhiteSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
                            shadowElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "መሰረታዊ መረጃ (Details)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandNavy
                                )
                                DetailItem("የተመዘገበበት ቀን", customer.registrationDate.substringBefore("T"))
                                DetailItem("የተመዘገበው በ", customer.createdBy)
                                val balanceText = if (customer.remainingBalanceAmount != 0.0) {
                                    "${customer.remainingBalanceAmount} ብር"
                                } else {
                                    "0 ብር (የተስተካከለ)"
                                }
                                DetailItem("አጠቃላይ ሂሳብ", balanceText)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Customer Notes Section
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Notes,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "የደንበኛ ማስታወሻዎች",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                TextButton(onClick = { showAddNoteDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ማስታወሻ አክል")
                                }
                            }

                            if (state.notes.isEmpty()) {
                                Text(
                                    text = "ምንም ማስታወሻ አልተገኘም።",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    state.notes.forEach { note ->
                                        CustomerNoteItem(note = note)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { navController.navigate(Screen.EditCustomer.createRoute(customer.id)) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen)
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ደንበኛ አርትዕ (Edit)")
                            }

                            Button(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorColor),
                                enabled = !isDeletingCustomer
                            ) {
                                if (isDeletingCustomer) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ሰርዝ (Delete)")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (showAddNoteDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isSavingNote) {
                        showAddNoteDialog = false
                        newNoteText = ""
                    }
                },
                title = { Text("ማስታወሻ አክል") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newNoteText,
                            onValueChange = { newNoteText = it },
                            label = { Text("የማስታወሻው ዝርዝር") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            enabled = !isSavingNote
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val text = newNoteText.trim()
                            if (text.isNotBlank() && !isSavingNote) {
                                isSavingNote = true
                                viewModel.addNote(text) { success, msg ->
                                    isSavingNote = false
                                    if (success) {
                                        showAddNoteDialog = false
                                        newNoteText = ""
                                    }
                                    scope.launch {
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }
                            }
                        },
                        enabled = newNoteText.isNotBlank() && !isSavingNote
                    ) {
                        if (isSavingNote) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("አስቀምጥ")
                        }
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showAddNoteDialog = false
                            newNoteText = ""
                        },
                        enabled = !isSavingNote
                    ) {
                        Text("ሰርዝ")
                    }
                }
            )
        }

        if (showDeleteDialog) {
            ConfirmDialog(
                title = "ደንበኛ ሰርዝ (Delete Customer)",
                message = "ይህንን ደንበኛ መሰረዝ ይፈልጋሉ? ከፋይናንስ እንቅስቃሴ ጋር የተያያዙ ደንበኞች በሂሳብ ሚዛን ጥበቃ ምክንያት ሊሰረዙ አይችሉም። (Customers with associated financial transactions cannot be deleted).",
                onConfirm = {
                    showDeleteDialog = false
                    isDeletingCustomer = true
                    viewModel.deleteCustomer { success, msg ->
                        isDeletingCustomer = false
                        if (success) {
                            navController.popBackStack()
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    }
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}

@Composable
private fun CustomerNoteItem(note: CustomerNote) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = PureWhiteSurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = note.note,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LightMint
                ) {
                    Text(
                        text = note.authorName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = note.date.substringBefore("T"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}
