package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.chuledigita.mobile.domain.model.Customer
import com.chuledigita.mobile.domain.model.Order
import com.chuledigita.mobile.domain.model.OrderStatus
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.navigation.Screen
import com.chuledigita.mobile.ui.theme.SuccessColor
import com.chuledigita.mobile.ui.theme.WarningColor
import kotlinx.coroutines.delay

/**
 * Main Orders screen connected to the real backend API.
 * Displays Active and Completed orders in two distinct tabs.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    navController: NavHostController,
    viewModel: OrdersViewModel = viewModel(factory = OrdersViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableStateOf(0) }
    var showAddBottomSheet by remember { mutableStateOf(false) }

    MainScaffold(
        navController = navController,
        title = "ትዕዛዞች"
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is OrdersUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is OrdersUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.message, 
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "እባክዎ ደግመው ይሞክሩ።", 
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Button(onClick = { viewModel.loadData() }, modifier = Modifier.padding(top = 16.dp)) {
                                Text("እንደገና ሞክር")
                            }
                        }
                    }
                }
                is OrdersUiState.Success -> {
                    val displayedOrders = if (selectedTab == 0) state.activeOrders else state.completedOrders
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Success Feedback Banner
                        state.successMessage?.let { msg ->
                            Surface(
                                color = SuccessColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SuccessColor.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessColor)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = msg, style = MaterialTheme.typography.bodyMedium, color = SuccessColor, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            LaunchedEffect(msg) {
                                delay(5000)
                                viewModel.resetSuccessMessage()
                            }
                        }

                        // Requirement 3: TWO tabs exactly (Active vs Completed)
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "የትዕዛዝ ዝርዝር",
                                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        if (state.activeOrders.isNotEmpty()) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.padding(start = 6.dp)
                                            ) {
                                                Text(
                                                    text = state.activeOrders.size.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "የተጠናቁ",
                                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        if (state.completedOrders.isNotEmpty()) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.padding(start = 6.dp)
                                            ) {
                                                Text(
                                                    text = state.completedOrders.size.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Orders list or Empty State
                        if (displayedOrders.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                val emptyText = if (selectedTab == 0) "ምንም ንቁ ትዕዛዝ የለም።" else "ምንም የተጠናቀቀ ትዕዛዝ የለም።"
                                Text(
                                    text = emptyText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(displayedOrders, key = { it.id }) { order ->
                                    OrderCardItem(order = order) {
                                        navController.navigate(Screen.OrderDetails.createRoute(order.id))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Requirement 4: FAB - Circular plus icon only
            FloatingActionButton(
                onClick = { showAddBottomSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "ትዕዛዝ አክል")
            }
        }

        // Add Order Bottom Sheet
        if (showAddBottomSheet) {
            AddOrderBottomSheet(
                customers = customers,
                onDismiss = { showAddBottomSheet = false },
                onOrderCreated = { customerId, title, details, size ->
                    viewModel.createOrder(customerId, title, details, size)
                    showAddBottomSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrderBottomSheet(
    customers: List<Customer>,
    onDismiss: () -> Unit,
    onOrderCreated: (customerId: String, title: String, details: String, size: String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
        var orderTitle by remember { mutableStateOf("") }
        var orderDescription by remember { mutableStateOf("") }
        var orderSize by remember { mutableStateOf("") }
        var isDropdownExpanded by remember { mutableStateOf(false) }

        var customerError by remember { mutableStateOf<String?>(null) }
        var orderTitleError by remember { mutableStateOf<String?>(null) }
        var orderDescriptionError by remember { mutableStateOf<String?>(null) }
        var orderSizeError by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "አዲስ ትዕዛዝ መዝግብ",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Requirement: Customer Selector
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedCustomer?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("ደንበኛ ይምረጡ") },
                    isError = customerError != null,
                    supportingText = { customerError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { isDropdownExpanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    customers.forEach { customer ->
                        DropdownMenuItem(
                            text = { Text(customer.name) },
                            onClick = {
                                selectedCustomer = customer
                                isDropdownExpanded = false
                                customerError = null
                            }
                        )
                    }
                }
                Box(modifier = Modifier.matchParentSize().clickable { isDropdownExpanded = true })
            }

            // Requirement 5: Exactly three order fields
            OutlinedTextField(
                value = orderTitle,
                onValueChange = { 
                    orderTitle = it
                    if (it.isNotBlank()) orderTitleError = null
                },
                label = { Text("የትዕዛዝ ርዕስ") },
                isError = orderTitleError != null,
                supportingText = { orderTitleError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = orderDescription,
                onValueChange = { 
                    orderDescription = it
                    if (it.isNotBlank()) orderDescriptionError = null
                },
                label = { Text("የትዕዛዝ ዝርዝር") },
                isError = orderDescriptionError != null,
                supportingText = { orderDescriptionError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = orderSize,
                onValueChange = { 
                    orderSize = it
                    if (it.isNotBlank()) orderSizeError = null
                },
                label = { Text("የትዕዛዝ መጠን") },
                isError = orderSizeError != null,
                supportingText = { orderSizeError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ሰርዝ")
                }

                Button(
                    onClick = {
                        var hasError = false
                        if (selectedCustomer == null) {
                            customerError = "ደንበኛ ይምረጡ።"
                            hasError = true
                        }
                        if (orderTitle.isBlank()) {
                            orderTitleError = "የትዕዛዝ ርዕስ ያስገቡ።"
                            hasError = true
                        }
                        if (orderDescription.isBlank()) {
                            orderDescriptionError = "የትዕዛዝ ዝርዝር ያስገቡ።"
                            hasError = true
                        }
                        if (orderSize.isBlank()) {
                            orderSizeError = "የትዕዛዝ መጠን ያስገቡ።"
                            hasError = true
                        }

                        if (!hasError) {
                            onOrderCreated(selectedCustomer!!.id, orderTitle, orderDescription, orderSize)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ትዕዛዝ መዝግብ")
                }
            }
        }
    }
}

@Composable
fun OrderCardItem(
    order: Order,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.orderNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OrderStatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = order.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            // Requirement 9: Completion information if completed
            if (order.status == OrderStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = SuccessColor.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SuccessColor.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ተጠናቋል በ:",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = order.completionActor ?: "-",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = SuccessColor.copy(alpha = 0.7f))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = order.completionDate ?: "-",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(12.dp), tint = SuccessColor.copy(alpha = 0.7f))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = order.completionTime ?: "-",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person, 
                            contentDescription = null, 
                            modifier = Modifier.size(12.dp), 
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        // Requirement 9: Creator name in bold
                        Text(
                            text = "በ: ${order.takenByFamilyMember}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday, 
                                contentDescription = null, 
                                modifier = Modifier.size(12.dp), 
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = order.orderDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime, 
                                contentDescription = null, 
                                modifier = Modifier.size(12.dp), 
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = order.orderTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun OrderStatusBadge(status: OrderStatus) {
    val (text, color) = when (status) {
        OrderStatus.PENDING -> "ትዕዛዝ ተመዝግቧል" to MaterialTheme.colorScheme.outline
        OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.READY -> "በሂደት ላይ" to WarningColor
        OrderStatus.COMPLETED -> "ተጠናቋል" to SuccessColor
        OrderStatus.CANCELLED -> "ተሰርዟል" to MaterialTheme.colorScheme.error
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
