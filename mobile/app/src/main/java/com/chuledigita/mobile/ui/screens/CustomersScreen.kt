package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.chuledigita.mobile.domain.model.CreditStatus
import com.chuledigita.mobile.domain.model.Customer
import com.chuledigita.mobile.domain.model.RemainingBalanceStatus
import com.chuledigita.mobile.ui.components.*
import com.chuledigita.mobile.ui.navigation.Screen
import com.chuledigita.mobile.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CustomersScreen(
    navController: NavHostController,
    viewModel: CustomersViewModel = viewModel(factory = CustomersViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showPaymentDialog by remember { mutableStateOf<Customer?>(null) }
    var showCollectionDialog by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }

    MainScaffold(
        navController = navController,
        title = "Customers"
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(OffWhiteBg)
        ) {
            when (val state = uiState) {
                is CustomersUiState.Loading -> LoadingState()
                is CustomersUiState.Empty -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ScreenHeaderBanner(
                            title = "Customers",
                            subtitle = "Manage your customers and build stronger relationships.",
                            illustration = { CustomersHeaderIllustration() },
                            action = {
                                IconButton(
                                    onClick = { viewModel.refresh() },
                                    modifier = Modifier
                                        .background(PureWhiteSurface, CircleShape)
                                        .border(1.dp, BorderDivider, CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            EmptyStatePlaceholder(
                                icon = Icons.Default.Search,
                                text = "No customers found",
                                subText = "Start by adding your first customer."
                            )
                        }
                    }
                }
                is CustomersUiState.Error -> {
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
                            onClick = { viewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("እንደገና ሞክር (Retry)")
                        }
                    }
                }
                is CustomersUiState.Success -> {
                    val searchFilteredCustomers = remember(state.customers, state.searchQuery) {
                        state.customers.filter {
                            it.name.contains(state.searchQuery, ignoreCase = true) ||
                            (it.phone?.contains(state.searchQuery) ?: false)
                        }
                    }

                    var showBulkDeleteDialog by remember { mutableStateOf(false) }

                    LaunchedEffect(state.actionMessage, state.actionError) {
                        state.actionMessage?.let {
                            snackbarHostState.showSnackbar(it)
                            viewModel.clearActionMessages()
                        }
                        state.actionError?.let {
                            snackbarHostState.showSnackbar(it)
                            viewModel.clearActionMessages()
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        ScreenHeaderBanner(
                            title = "Customers",
                            subtitle = "Manage your customers and build stronger relationships.",
                            illustration = { CustomersHeaderIllustration() },
                            action = {
                                IconButton(
                                    onClick = { viewModel.refresh() },
                                    modifier = Modifier
                                        .background(PureWhiteSurface, CircleShape)
                                        .border(1.dp, BorderDivider, CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )

                        MultiSelectActionBar(
                            selectedCount = state.selectedIds.size,
                            totalCount = searchFilteredCustomers.size,
                            isDeleting = state.isDeleting,
                            onSelectAll = {
                                viewModel.selectAllCustomers(searchFilteredCustomers.map { it.id })
                            },
                            onClearSelection = {
                                viewModel.clearCustomerSelection()
                            },
                            onDeleteSelected = {
                                showBulkDeleteDialog = true
                            }
                        )

                        CustomersList(
                            state = state,
                            filteredCustomers = searchFilteredCustomers,
                            onSearchQueryChanged = viewModel::onSearchQueryChanged,
                            onCustomerClick = { navController.navigate(Screen.CustomerDetails.createRoute(it.id)) },
                            onEditClick = { navController.navigate(Screen.EditCustomer.createRoute(it.id)) },
                            onAddCustomerClick = { navController.navigate(Screen.AddCustomer.route) },
                            onPayCredit = { showPaymentDialog = it },
                            onCollectBalance = { showCollectionDialog = it },
                            onToggleSelect = viewModel::toggleSelectCustomer,
                            onDeleteCustomer = { customerToDelete = it }
                        )
                    }

                    if (showBulkDeleteDialog) {
                        MultiSelectDeleteConfirmationDialog(
                            itemType = "customer",
                            selectedCount = state.selectedIds.size,
                            warningMessage = "Customers with associated financial transactions will be preserved for accounting audit integrity.",
                            isDeleting = state.isDeleting,
                            onConfirm = {
                                viewModel.bulkDeleteSelectedCustomers { _, _, _ ->
                                    showBulkDeleteDialog = false
                                }
                            },
                            onDismiss = {
                                if (!state.isDeleting) showBulkDeleteDialog = false
                            }
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddCustomer.route) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                containerColor = EmeraldGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Single Customer Delete Confirmation Dialog
        customerToDelete?.let { customer ->
            ConfirmDialog(
                title = "ደንበኛ ሰርዝ (Delete Customer)",
                message = "ደንበኛ \"${customer.name}\" መሰረዝ ይፈልጋሉ? ከፋይናንስ እንቅስቃሴ ጋር የተያያዙ ደንበኞች በሂሳብ ሚዛን ጥበቃ ምክንያት ሊሰረዙ አይችሉም።",
                onConfirm = {
                    viewModel.deleteCustomer(customer.id)
                    customerToDelete = null
                },
                onDismiss = { customerToDelete = null }
            )
        }

        // Debt Payment Flow
        showPaymentDialog?.let { customer ->
            CreditPaymentDialog(
                customerName = customer.name,
                creditAmount = customer.creditAmount,
                onConfirm = {
                    showPaymentDialog = null
                    scope.launch {
                        snackbarHostState.showSnackbar("ክፍያዎችን በፋይናንስ እንቅስቃሴ ክፍል ውስጥ ማስተናገድ ይችላሉ።")
                    }
                },
                onDismiss = { showPaymentDialog = null }
            )
        }

        // Remaining Balance Flow
        showCollectionDialog?.let { customer ->
            RemainingBalanceCollectionDialog(
                customerName = customer.name,
                balanceAmount = customer.remainingBalanceAmount,
                onConfirm = {
                    showCollectionDialog = null
                    scope.launch {
                        snackbarHostState.showSnackbar("የቀሪ ሂሳብ እንቅስቃሴዎችን በፋይናንስ ክፍል ውስጥ ማስተናገድ ይችላሉ።")
                    }
                },
                onDismiss = { showCollectionDialog = null }
            )
        }
    }
}

@Composable
fun CustomersList(
    state: CustomersUiState.Success,
    filteredCustomers: List<Customer>,
    onSearchQueryChanged: (String) -> Unit,
    onCustomerClick: (Customer) -> Unit,
    onEditClick: (Customer) -> Unit,
    onAddCustomerClick: () -> Unit,
    onPayCredit: (Customer) -> Unit,
    onCollectBalance: (Customer) -> Unit,
    onToggleSelect: (String) -> Unit,
    onDeleteCustomer: (Customer) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }

    val activeCount = remember(filteredCustomers) {
        filteredCustomers.count {
            (it.creditAmount > 0.0 && it.creditStatus != CreditStatus.PAID_CREDIT) ||
            (it.remainingBalanceAmount > 0.0 && it.remainingBalanceStatus != RemainingBalanceStatus.BALANCE_COLLECTED) ||
            it.registrationDate.isNotBlank()
        }
    }
    val inactiveCount = remember(filteredCustomers, activeCount) {
        (filteredCustomers.size - activeCount).coerceAtLeast(0)
    }

    val displayList = remember(filteredCustomers, selectedFilter) {
        when (selectedFilter) {
            "Active" -> filteredCustomers.filter {
                (it.creditAmount > 0.0 && it.creditStatus != CreditStatus.PAID_CREDIT) ||
                (it.remainingBalanceAmount > 0.0 && it.remainingBalanceStatus != RemainingBalanceStatus.BALANCE_COLLECTED) ||
                it.registrationDate.isNotBlank()
            }
            "Inactive" -> filteredCustomers.filter {
                !((it.creditAmount > 0.0 && it.creditStatus != CreditStatus.PAID_CREDIT) ||
                  (it.remainingBalanceAmount > 0.0 && it.remainingBalanceStatus != RemainingBalanceStatus.BALANCE_COLLECTED) ||
                  it.registrationDate.isNotBlank())
            }
            else -> filteredCustomers
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Add Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search customers...", color = TextSecondary, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(20.dp)
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PureWhiteSurface,
                    unfocusedContainerColor = PureWhiteSurface,
                    focusedBorderColor = EmeraldGreen,
                    unfocusedBorderColor = BorderDivider
                ),
                singleLine = true
            )

            Button(
                onClick = onAddCustomerClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            }
        }

        // Filter Pills: All, Active, Inactive (Matching reference board)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomerFilterChip(
                label = "All",
                count = filteredCustomers.size,
                isSelected = selectedFilter == "All",
                onClick = { selectedFilter = "All" }
            )
            CustomerFilterChip(
                label = "Active",
                count = activeCount,
                isSelected = selectedFilter == "Active",
                onClick = { selectedFilter = "Active" }
            )
            CustomerFilterChip(
                label = "Inactive",
                count = inactiveCount,
                isSelected = selectedFilter == "Inactive",
                onClick = { selectedFilter = "Inactive" }
            )
        }

        if (displayList.isEmpty()) {
            EmptyStatePlaceholder(
                icon = Icons.Default.Search,
                text = "No results",
                subText = "Try a different search term or filter."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayList, key = { it.id }) { customer ->
                    CustomerCard(
                        customer = customer,
                        onClick = { onCustomerClick(customer) },
                        onEditClick = { onEditClick(customer) },
                        onPayCredit = { onPayCredit(customer) },
                        onCollectBalance = { onCollectBalance(customer) },
                        isEditable = true,
                        isSelected = state.selectedIds.contains(customer.id),
                        isSelectionActive = state.selectedIds.isNotEmpty(),
                        onToggleSelect = { onToggleSelect(customer.id) },
                        onDeleteClick = { onDeleteCustomer(customer) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerFilterChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) EmeraldGreen else PureWhiteSurface,
        border = BorderStroke(
            1.dp,
            if (isSelected) EmeraldGreen else BorderDivider
        ),
        shadowElevation = if (isSelected) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextPrimary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = CircleShape,
                color = if (isSelected) Color.White.copy(alpha = 0.25f) else LightMint
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else EmeraldGreen,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
    }
}
