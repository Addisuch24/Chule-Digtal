package com.chuledigita.mobile.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.chuledigita.mobile.domain.model.FinancialMovement
import com.chuledigita.mobile.domain.model.MovementType
import com.chuledigita.mobile.ui.components.ConfirmDialog
import com.chuledigita.mobile.ui.components.FinanceHeaderIllustration
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.components.ScreenHeaderBanner
import com.chuledigita.mobile.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Redesigned Finance screen matching the Chule Digital web experience.
 * Features a movement record form and a shared ledger grouped by balance status.
 */
@Composable
fun FinanceScreen(
    navController: NavHostController,
    onLogout: () -> Unit = {},
    viewModel: FinanceViewModel = viewModel(factory = FinanceViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    MainScaffold(
        navController = navController,
        title = "ፋይናንስ",
        onLogout = onLogout
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmOffWhite)
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is FinanceUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandGreen)
                    }
                }
                is FinanceUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(state.message, color = ErrorColor, textAlign = TextAlign.Center)
                        Button(
                            onClick = { viewModel.loadData() },
                            modifier = Modifier.padding(top = 16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                        ) {
                            Text("እንደገና ሞክር")
                        }
                    }
                }
                is FinanceUiState.Success -> {
                    FinanceContent(
                        state = state,
                        onRecord = viewModel::recordEntry,
                        onSearch = viewModel::onSearchQueryChanged,
                        onDelete = viewModel::deleteMovement,
                        onUpdate = viewModel::updateMovement,
                        onDeleteCustomer = viewModel::deleteCustomer,
                        onClearMessages = viewModel::clearMessages
                    )
                }
            }

            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
fun FinanceContent(
    state: FinanceUiState.Success,
    onRecord: (String, String?, Double, String) -> Unit,
    onSearch: (String) -> Unit,
    onDelete: (String) -> Unit,
    onUpdate: (String, String, Double, String) -> Unit,
    onDeleteCustomer: (String, String) -> Unit,
    onClearMessages: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            FinanceHeader(movementCount = state.totalMovementCount)
        }

        // 3 Finance Summary Cards (Total Balance, Total Debit, Total Credit)
        item {
            val totalDebit = state.customersWhoOwe.sumOf { kotlin.math.abs(it.balance) }
            val totalCredit = state.customersWithCredit.sumOf { it.balance }
            val netBalance = totalCredit - totalDebit
            val isNegative = netBalance < 0
            val balancePrefix = if (netBalance > 0) "+ " else if (isNegative) "− " else ""
            val balanceColor = if (isNegative) ErrorColor else EmeraldGreen

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinanceMetricMiniCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Balance",
                    value = "$balancePrefix${kotlin.math.abs(netBalance).toInt()} ETB",
                    valueColor = balanceColor,
                    isPositive = !isNegative
                )
                FinanceMetricMiniCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Debit",
                    value = "− ${totalDebit.toInt()} ETB",
                    valueColor = ErrorColor,
                    isPositive = false
                )
                FinanceMetricMiniCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Credit",
                    value = "+ ${totalCredit.toInt()} ETB",
                    valueColor = EmeraldGreen,
                    isPositive = true
                )
            }
        }

        item {
            RecordEntryForm(
                isSaving = state.isSaving,
                onSave = onRecord,
                successMessage = state.successMessage,
                errorMessage = state.errorMessage,
                onDismissMessage = onClearMessages
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = PureWhiteSurface,
                border = BorderStroke(1.dp, BorderDivider),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column {
                            Text(
                                text = "SHARED LEDGER",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandGold,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Customer balances",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen
                            )
                        }
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearch,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search movements by customer, d...", fontSize = 12.sp, color = BrandGreen.copy(alpha = 0.4f)) },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(18.dp)) },
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

                    if (state.customersWhoOwe.isEmpty() && state.customersWithCredit.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = BrandGreen.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("ምንም የሂሳብ መረጃ አልተገኘም።", color = BrandGreen.copy(alpha = 0.5f))
                            }
                        }
                    } else {
                        // Stacked vertically for mobile screens: Debtors above, Creditors below
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Customers who owe (Debtors)
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Surface(
                                        color = ErrorColor.copy(alpha = 0.1f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Remove, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ዕዳ ያለባቸው ደንበኞች",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ErrorColor
                                    )
                                }

                                if (state.customersWhoOwe.isEmpty()) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = CreamSurface,
                                        border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.05f))
                                    ) {
                                        Text(
                                            text = "ምንም ዕዳ የለም",
                                            modifier = Modifier.padding(16.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = BrandGreen.copy(alpha = 0.4f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    state.customersWhoOwe.forEach { balance ->
                                        CustomerBalanceItem(
                                            balance = balance,
                                            onDelete = onDelete,
                                            onUpdate = onUpdate,
                                            onDeleteCustomer = onDeleteCustomer
                                        )
                                    }
                                }
                            }

                            // Customers with credit (Creditors)
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Surface(
                                        color = SuccessColor.copy(alpha = 0.1f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ቀሪ ብር ያላቸው ደንበኞች",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessColor
                                    )
                                }

                                if (state.customersWithCredit.isEmpty()) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = CreamSurface,
                                        border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.05f))
                                    ) {
                                        Text(
                                            text = "ምንም ቀሪ ሂሳብ የለም",
                                            modifier = Modifier.padding(16.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = BrandGreen.copy(alpha = 0.4f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    state.customersWithCredit.forEach { balance ->
                                        CustomerBalanceItem(
                                            balance = balance,
                                            onDelete = onDelete,
                                            onUpdate = onUpdate,
                                            onDeleteCustomer = onDeleteCustomer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
fun FinanceHeader(movementCount: Int) {
    ScreenHeaderBanner(
        title = "Finance",
        subtitle = "Track income, expenses and keep your balance healthy.",
        illustration = {
            FinanceHeaderIllustration()
        }
    )
}

@Composable
private fun FinanceMetricMiniCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    valueColor: Color,
    isPositive: Boolean
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PureWhiteSurface,
        border = BorderStroke(1.dp, BorderDivider),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    color = if (isPositive) LightMint else ErrorColor.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (isPositive) EmeraldGreen else ErrorColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RecordEntryForm(
    isSaving: Boolean,
    onSave: (String, String?, Double, String) -> Unit,
    successMessage: String?,
    errorMessage: String?,
    onDismissMessage: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.08f)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("ዕዳ ወይም ክፍያ መዝግብ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrandGreen)

            if (successMessage != null || errorMessage != null) {
                val color = if (successMessage != null) SuccessColor else ErrorColor
                Surface(
                    color = color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = successMessage ?: errorMessage ?: "",
                        color = color,
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                LaunchedEffect(successMessage, errorMessage) {
                    delay(5000)
                    onDismissMessage()
                    if (successMessage != null) {
                        name = ""; phone = ""; amountText = ""; description = ""
                    }
                }
            }

            // Stacked vertically in Amharic for non-professional users:
            // 1. Customer name
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("የደንበኛ ስም", style = MaterialTheme.typography.labelMedium, color = BrandGreen.copy(alpha = 0.7f))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    placeholder = { Text("ስም ያስገቡ", color = BrandGreen.copy(alpha = 0.3f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = BrandGreen.copy(alpha = 0.2f),
                        focusedContainerColor = CreamSurface,
                        unfocusedContainerColor = CreamSurface
                    ),
                    singleLine = true
                )
            }

            // 2. Phone
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ስልክ ቁጥር (አማራጭ)", style = MaterialTheme.typography.labelMedium, color = BrandGreen.copy(alpha = 0.7f))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    placeholder = { Text("09...", color = BrandGreen.copy(alpha = 0.3f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = BrandGreen.copy(alpha = 0.2f),
                        focusedContainerColor = CreamSurface,
                        unfocusedContainerColor = CreamSurface
                    ),
                    singleLine = true
                )
            }

            // 3. Amount
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("መጠን (ብር)", style = MaterialTheme.typography.labelMedium, color = BrandGreen.copy(alpha = 0.7f))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    placeholder = { Text("-100 ወይም 100", color = BrandGreen.copy(alpha = 0.3f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = BrandGreen.copy(alpha = 0.2f),
                        focusedContainerColor = CreamSurface,
                        unfocusedContainerColor = CreamSurface
                    ),
                    singleLine = true
                )
            }

            // 4. Description
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ማብራሪያ (አማራጭ)", style = MaterialTheme.typography.labelMedium, color = BrandGreen.copy(alpha = 0.7f))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    placeholder = { Text("አጭር ማስታወሻ", color = BrandGreen.copy(alpha = 0.3f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = BrandGreen.copy(alpha = 0.2f),
                        focusedContainerColor = CreamSurface,
                        unfocusedContainerColor = CreamSurface
                    ),
                    singleLine = true
                )
            }

            Text(
                text = "የመቀነስ (-) ምልክት ሲያስገቡ ዕዳ ይሆናል፤ ያለ ምልክት (ወይም +) ሲያስገቡ ክፍያ/ቀሪ ብር ይሆናል።",
                style = MaterialTheme.typography.bodySmall,
                color = BrandGreen.copy(alpha = 0.6f)
            )

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (name.isNotBlank() && amount != null) {
                        onSave(name, phone.ifBlank { null }, amount, description)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C5E6B)),
                enabled = !isSaving && name.isNotBlank() && amountText.isNotBlank()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("መዝግብ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun CustomerBalanceItem(
    balance: CustomerBalance,
    onDelete: (String) -> Unit,
    onUpdate: (String, String, Double, String) -> Unit,
    onDeleteCustomer: (String, String) -> Unit = { _, _ -> }
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteCustomerConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CreamSurface,
        border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.06f)),
        shadowElevation = 1.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = balance.customerName, fontWeight = FontWeight.Bold, color = BrandGreen)
                    if (balance.customerPhone != null) {
                        Text(text = balance.customerPhone, style = MaterialTheme.typography.bodySmall, color = BrandGreen.copy(alpha = 0.5f))
                    }
                }

                val color = if (balance.balance < 0) ErrorColor else SuccessColor
                val prefix = if (balance.balance < 0) "-" else "+"
                Text(
                    text = "$prefix ${kotlin.math.abs(balance.balance).toInt()} ETB",
                    color = color,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = { showDeleteCustomerConfirm = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "ደንበኛውን ሰርዝ",
                        tint = ErrorColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = BrandGreen.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (showDeleteCustomerConfirm) {
                ConfirmDialog(
                    title = "ደንበኛውን ሰርዝ",
                    message = "ደንበኛውን '${balance.customerName}' እና ሁሉንም ተያያዥ የሂሳብ መረጃዎች መሰረዝ እንደሚፈልጉ እርግጠኛ ነዎት? ይህም በዳሽቦርድ ላይ የሚታየውን የደንበኞች ቁጥር በአንድ ይቀንሳል።",
                    onConfirm = {
                        onDeleteCustomer(balance.customerId, balance.customerName)
                        showDeleteCustomerConfirm = false
                    },
                    onDismiss = { showDeleteCustomerConfirm = false }
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarmOffWhite.copy(alpha = 0.7f))
                        .padding(bottom = 10.dp)
                ) {
                    HorizontalDivider(color = BrandGreen.copy(alpha = 0.05f))
                    Column(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    ) {
                        balance.movements.forEachIndexed { index, movement ->
                            MovementRow(
                                movement = movement,
                                onDelete = { onDelete(movement.id) },
                                onUpdate = { amt, desc -> onUpdate(movement.id, movement.customerId, amt, desc) }
                            )
                            if (index < balance.movements.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 10.dp), color = Color.LightGray.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MovementRow(
    movement: FinancialMovement,
    onDelete: () -> Unit,
    onUpdate: (Double, String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = movement.description, style = MaterialTheme.typography.bodyMedium, color = BrandGreen)
            Spacer(modifier = Modifier.height(2.dp))
            // Labeled by who took the action and date/time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = BrandGreen.copy(alpha = 0.4f), modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "በ ${movement.recordedBy}",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandGreen.copy(alpha = 0.5f)
                )
                Text(
                    text = " • ${movement.date}",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandGreen.copy(alpha = 0.5f)
                )
            }
        }

        val amountColor = if (movement.type == MovementType.DEBIT) ErrorColor else SuccessColor
        val sign = if (movement.type == MovementType.DEBIT) "-" else "+"
        Text(
            text = "$sign ${movement.amount.toInt()} ETB",
            color = amountColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Three-dots menu hidden on the rightmost
        Box {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = BrandGreen.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("አስተካክል (Edit)") },
                    onClick = {
                        menuExpanded = false
                        showEditDialog = true
                    },
                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = BrandGreen) }
                )
                DropdownMenuItem(
                    text = { Text("ሰርዝ (Delete)", color = ErrorColor) },
                    onClick = {
                        menuExpanded = false
                        showDeleteConfirm = true
                    },
                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = ErrorColor) }
                )
            }
        }
    }

    if (showEditDialog) {
        EditMovementDialog(
            movement = movement,
            onDismiss = { showEditDialog = false },
            onConfirm = { amt, desc ->
                onUpdate(amt, desc)
                showEditDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "እንቅስቃሴውን ሰርዝ",
            message = "ይህንን የፋይናንስ እንቅስቃሴ መሰረዝ እንደሚፈልጉ እርግጠኛ ነዎት?",
            onConfirm = {
                onDelete()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
fun EditMovementDialog(
    movement: FinancialMovement,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    val initialAmount = if (movement.type == MovementType.DEBIT) -movement.amount else movement.amount
    var amountText by remember { mutableStateOf(initialAmount.toInt().toString()) }
    var description by remember { mutableStateOf(movement.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("እንቅስቃሴውን አስተካክል (Edit Entry)", color = BrandGreen) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Clearly labeled with who took the action and date/time on the edit page
                Surface(
                    color = CreamSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "የተመዘገበው በ: ${movement.recordedBy}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandGreen,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "ቀን እና ሰዓት: ${movement.date}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandGreen.copy(alpha = 0.7f)
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("መጠን (ብር)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandGreen)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("መግለጫ") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandGreen)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt != null) onConfirm(amt, description)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) {
                Text("አስቀምጥ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ሰርዝ", color = BrandGreen)
            }
        }
    )
}
