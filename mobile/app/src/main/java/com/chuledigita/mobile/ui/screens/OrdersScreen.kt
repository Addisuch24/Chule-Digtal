package com.chuledigita.mobile.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.chuledigita.mobile.domain.model.Order
import com.chuledigita.mobile.domain.model.OrderStatus
import com.chuledigita.mobile.ui.components.ConfirmDialog
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    navController: NavHostController,
    viewModel: OrdersViewModel = viewModel(factory = OrdersViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MainScaffold(
        navController = navController,
        title = "ትዕዛዞች",
        showBottomBar = true
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmOffWhite)
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is OrdersUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandGreen)
                    }
                }

                is OrdersUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = ErrorColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            color = ErrorColor,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadData() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                        ) {
                            Text("እንደገና ሞክር")
                        }
                    }
                }

                is OrdersUiState.Success -> {
                    OrdersContent(
                        state = state,
                        onCreateOrder = viewModel::createOrder,
                        onUpdateOrder = viewModel::updateOrder,
                        onUpdateStatus = viewModel::updateOrderStatus,
                        onDeleteOrder = viewModel::deleteOrder,
                        onStartEditing = viewModel::startEditing,
                        onCancelEditing = viewModel::cancelEditing,
                        onSearch = viewModel::onSearchQueryChanged,
                        onClearMessage = viewModel::clearMessage
                    )
                }
            }
        }
    }
}

@Composable
private fun OrdersContent(
    state: OrdersUiState.Success,
    onCreateOrder: (String, String, String?) -> Unit,
    onUpdateOrder: (String, String?, String?) -> Unit,
    onUpdateStatus: (String, OrderStatus) -> Unit,
    onDeleteOrder: (String) -> Unit,
    onStartEditing: (Order) -> Unit,
    onCancelEditing: () -> Unit,
    onSearch: (String) -> Unit,
    onClearMessage: () -> Unit
) {
    var orderToDelete by remember { mutableStateOf<Order?>(null) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            OrdersHeader(totalCount = state.orders.size)
        }

        // Inline Feedback
        if (state.successMessage != null || state.errorMessage != null) {
            item {
                val isSuccess = state.successMessage != null
                val msg = state.successMessage ?: state.errorMessage ?: ""
                val bg = if (isSuccess) SuccessColor.copy(alpha = 0.1f) else ErrorColor.copy(alpha = 0.1f)
                val stroke = if (isSuccess) SuccessColor.copy(alpha = 0.3f) else ErrorColor.copy(alpha = 0.3f)
                val textColor = if (isSuccess) SuccessColor else ErrorColor

                Surface(
                    color = bg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, stroke),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = msg,
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                LaunchedEffect(state.successMessage, state.errorMessage) {
                    kotlinx.coroutines.delay(4000)
                    onClearMessage()
                }
            }
        }

        // Card 1: Create an order form card in Amharic
        item {
            CreateOrderCard(
                editingOrder = state.editingOrder,
                isSubmitting = state.isSubmitting,
                successMessage = state.successMessage,
                onSave = { title, details ->
                    if (state.editingOrder != null) {
                        onUpdateOrder(state.editingOrder.id, title, details)
                    } else {
                        onCreateOrder(title, details, null)
                    }
                },
                onCancel = onCancelEditing
            )
        }

        // Card 2: Shared Queue / All orders in Amharic
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.08f)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "የተያዙ ትዕዛዞች",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandGold,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "ሁሉም ትዕዛዞች",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGreen
                                )
                            }
                        }

                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearch,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "በትዕዛዝ ቁጥር ወይም በደንበኛ ስም ፈልግ...",
                                    color = BrandGreen.copy(alpha = 0.4f),
                                    fontSize = 12.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = BrandGreen.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandGreen.copy(alpha = 0.3f),
                                unfocusedBorderColor = BrandGreen.copy(alpha = 0.1f),
                                focusedContainerColor = CreamSurface,
                                unfocusedContainerColor = CreamSurface,
                                cursorColor = BrandGreen
                            ),
                            singleLine = true
                        )
                    }

                    if (state.orders.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ምንም ትዕዛዝ አልተገኘም።",
                                color = BrandGreen.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            state.orders.forEach { order ->
                                OrderCardItem(
                                    order = order,
                                    onClick = { selectedOrder = order },
                                    onEdit = { onStartEditing(order) },
                                    onStatusChange = { newStatus -> onUpdateStatus(order.id, newStatus) },
                                    onDelete = { orderToDelete = order }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Selected Order Notepad Reader Dialog
    selectedOrder?.let { order ->
        OrderReaderDialog(
            order = order,
            onDismiss = { selectedOrder = null },
            onEdit = {
                selectedOrder = null
                onStartEditing(order)
            }
        )
    }

    // Delete Confirmation Dialog
    orderToDelete?.let { order ->
        ConfirmDialog(
            title = "ትዕዛዙን ሰርዝ",
            message = "\"${order.title ?: order.orderNumber}\" መሰረዝ እንደሚፈልጉ እርግጠኛ ነዎት? ይህ ድርጊት አይቀለበስም።",
            onConfirm = {
                onDeleteOrder(order.id)
                orderToDelete = null
            },
            onDismiss = { orderToDelete = null }
        )
    }
}

@Composable
private fun OrdersHeader(totalCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "ትዕዛዞች",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = BrandGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "የተደረጉ ስራዎችን እና ትዕዛዞችን በአንድ ላይ ይከታተሉ።",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandGreen.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = Color(0xFFE2F0F9),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "$totalCount ትዕዛዞች",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2980B9)
            )
        }
    }
}

@Composable
private fun CreateOrderCard(
    editingOrder: Order?,
    isSubmitting: Boolean,
    successMessage: String?,
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember(editingOrder) { mutableStateOf(editingOrder?.title ?: "") }
    var details by remember(editingOrder) { mutableStateOf(editingOrder?.description ?: "") }

    LaunchedEffect(successMessage) {
        if (successMessage != null && editingOrder == null) {
            title = ""
            details = ""
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.08f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "አዲስ ስራ",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandGold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (editingOrder != null) "ትዕዛዝ አስተካክል" else "ትዕዛዝ መዝግብ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreen
                    )
                }

                if (editingOrder != null) {
                    TextButton(onClick = onCancel) {
                        Text("ሰርዝ", color = BrandGreen)
                    }
                }
            }

            // Order Title Field
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "የትዕዛዝ ርዕስ (ስም)",
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandGreen.copy(alpha = 0.7f)
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "የስራውን ወይም የእቃውን ስም ይጻፉ",
                            color = BrandGreen.copy(alpha = 0.35f),
                            fontSize = 14.sp
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = BrandGreen.copy(alpha = 0.2f),
                        focusedContainerColor = CreamSurface,
                        unfocusedContainerColor = CreamSurface,
                        cursorColor = BrandGreen
                    ),
                    singleLine = true
                )
            }

            // Details Field
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ዝርዝር መግለጫ",
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandGreen.copy(alpha = 0.7f)
                )
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp),
                    placeholder = {
                        Text(
                            "ለምሳሌ፦ 24 ደብተሮች እና ወደ ቦሌ ማድረስ",
                            color = BrandGreen.copy(alpha = 0.35f),
                            fontSize = 14.sp
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = BrandGreen.copy(alpha = 0.2f),
                        focusedContainerColor = CreamSurface,
                        unfocusedContainerColor = CreamSurface,
                        cursorColor = BrandGreen
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                )
            }

            Button(
                onClick = {
                    if (title.isNotBlank() && details.isNotBlank() && !isSubmitting) {
                        onSave(title, details)
                    }
                },
                enabled = title.isNotBlank() && details.isNotBlank() && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2C5E6B),
                    contentColor = Color.White
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (editingOrder != null) "ለውጥ አስቀምጥ" else "ትዕዛዝ መዝግብ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderCardItem(
    order: Order,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onStatusChange: (OrderStatus) -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isCompleted = order.status == OrderStatus.COMPLETED

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isCompleted) CreamSurface.copy(alpha = 0.6f) else CreamSurface,
        border = BorderStroke(1.dp, if (isCompleted) ErrorColor.copy(alpha = 0.2f) else BrandGreen.copy(alpha = 0.06f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Pending: Bolded. Completed: Unbolded, faded with red color.
                    Text(
                        text = order.title ?: order.orderNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.Bold,
                        color = if (isCompleted) ErrorColor.copy(alpha = 0.7f) else BrandGreen,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = order.customerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCompleted) ErrorColor.copy(alpha = 0.5f) else BrandGreen.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = order.orderNumber,
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandGold,
                            fontWeight = FontWeight.Bold
                        )
                        if (isCompleted) {
                            Surface(
                                color = ErrorColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "ተጠናቋል (Completed)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ErrorColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Three-dots menu hidden on the rightmost
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "አማራጮች",
                            tint = BrandGreen.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("አስተካክል") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = BrandGreen) }
                        )
                        if (!isCompleted) {
                            DropdownMenuItem(
                                text = {
                                    Text("እንደተጠናቀቀ ምልክት አድርግ")
                                },
                                onClick = {
                                    menuExpanded = false
                                    onStatusChange(OrderStatus.COMPLETED)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SuccessColor
                                    )
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("ሰርዝ", color = ErrorColor) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = ErrorColor) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderReaderDialog(
    order: Order,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),
            shape = RoundedCornerShape(20.dp),
            color = CreamSurface,
            border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.1f)),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Notepad Header
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = order.orderNumber,
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandGold,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "ዝጋ", tint = BrandGreen)
                        }
                    }

                    Text(
                        text = order.title ?: order.orderNumber,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreen
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ደንበኛ: ${order.customerName} • ${order.orderDate} ${order.orderTime}".trim(),
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandGreen.copy(alpha = 0.5f)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = BrandGreen.copy(alpha = 0.1f))
                }

                // Notepad Body (Paper-like view for pure reading)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, BrandGreen.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = order.description.ifBlank { "ምንም ዝርዝር መግለጫ የለም" },
                                style = MaterialTheme.typography.bodyLarge,
                                color = BrandGreen,
                                lineHeight = 26.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("አስተካክል (Edit)", color = BrandGreen, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("ዝጋ (Close)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
