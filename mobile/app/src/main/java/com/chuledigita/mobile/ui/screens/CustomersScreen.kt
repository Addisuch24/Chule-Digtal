package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.chuledigita.mobile.data.mock.MockDataProvider
import com.chuledigita.mobile.domain.model.CreditStatus
import com.chuledigita.mobile.domain.model.Customer
import com.chuledigita.mobile.domain.model.RemainingBalanceStatus
import com.chuledigita.mobile.ui.components.*
import com.chuledigita.mobile.ui.navigation.Screen
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

    MainScaffold(
        navController = navController,
        title = "Customers"
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = uiState) {
                is CustomersUiState.Loading -> LoadingState()
                is CustomersUiState.Empty -> {
                    EmptyStatePlaceholder(
                        icon = Icons.Default.Search,
                        text = "No customers found",
                        subText = "Start by adding your first customer."
                    )
                }
                is CustomersUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is CustomersUiState.Success -> {
                    CustomersList(
                        state = state,
                        onSearchQueryChanged = viewModel::onSearchQueryChanged,
                        onCustomerClick = { navController.navigate(Screen.CustomerDetails.createRoute(it.id)) },
                        onEditClick = { navController.navigate(Screen.EditCustomer.createRoute(it.id)) },
                        onPayCredit = { showPaymentDialog = it },
                        onCollectBalance = { showCollectionDialog = it }
                    )
                }
            }

            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddCustomer.route) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Debt Payment Flow
        showPaymentDialog?.let { customer ->
            CreditPaymentDialog(
                customerName = customer.name,
                creditAmount = customer.creditAmount,
                onConfirm = {
                    MockDataProvider.updateCreditStatus(customer.id, CreditStatus.PAID_CREDIT)
                    viewModel.refresh()
                    showPaymentDialog = null
                    scope.launch {
                        snackbarHostState.showSnackbar("የደንበኛው ዕዳ ተከፍሏል። Paid: ${customer.creditAmount.toInt()} ETB")
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
                    MockDataProvider.updateRemainingBalanceStatus(customer.id, RemainingBalanceStatus.BALANCE_COLLECTED)
                    viewModel.refresh()
                    showCollectionDialog = null
                    scope.launch {
                        snackbarHostState.showSnackbar("የደንበኛው ቀሪ ሂሳብ ተሰጥቷል። Received: ${customer.remainingBalanceAmount.toInt()} ETB")
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
    onSearchQueryChanged: (String) -> Unit,
    onCustomerClick: (Customer) -> Unit,
    onEditClick: (Customer) -> Unit,
    onPayCredit: (Customer) -> Unit,
    onCollectBalance: (Customer) -> Unit
) {
    val filteredCustomers = remember(state.customers, state.searchQuery) {
        state.customers.filter { 
            it.name.contains(state.searchQuery, ignoreCase = true) || 
            (it.phone?.contains(state.searchQuery) ?: false)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search customers...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp)
        )

        if (filteredCustomers.isEmpty()) {
            EmptyStatePlaceholder(
                icon = Icons.Default.Search,
                text = "No results",
                subText = "Try a different search term."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredCustomers) { customer ->
                    CustomerCard(
                        customer = customer,
                        onClick = { onCustomerClick(customer) },
                        onEditClick = { onEditClick(customer) },
                        onPayCredit = { onPayCredit(customer) },
                        onCollectBalance = { onCollectBalance(customer) },
                        isEditable = customer.createdBy == MockDataProvider.currentFamilyMember.name
                    )
                }
            }
        }
    }
}
