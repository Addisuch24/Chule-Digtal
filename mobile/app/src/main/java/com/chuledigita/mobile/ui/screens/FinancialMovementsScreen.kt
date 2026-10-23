package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.chuledigita.mobile.domain.model.FinancialMovement
import com.chuledigita.mobile.domain.model.MovementType
import com.chuledigita.mobile.domain.model.VerificationStatus
import com.chuledigita.mobile.ui.components.EmptyStatePlaceholder
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.components.MultiSelectActionBar
import com.chuledigita.mobile.ui.components.MultiSelectDeleteConfirmationDialog
import com.chuledigita.mobile.ui.components.VerificationStatusBadge
import com.chuledigita.mobile.ui.navigation.Screen
import com.chuledigita.mobile.ui.theme.*

@Composable
fun FinancialMovementsScreen(
    navController: NavHostController,
    viewModel: FinancialMovementsViewModel = viewModel(factory = FinancialMovementsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    MainScaffold(
        navController = navController,
        title = "Movement History",
        showBottomBar = false
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(OffWhiteBg)
        ) {
            when (val state = uiState) {
                is FinancialMovementsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldGreen)
                    }
                }

                is FinancialMovementsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = ErrorColor,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = state.message, color = ErrorColor)
                            Button(
                                onClick = { viewModel.loadMovements() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("እንደገና ሞክር (Retry)")
                            }
                        }
                    }
                }

                is FinancialMovementsUiState.Success -> {
                    LaunchedEffect(state.actionMessage, state.actionError) {
                        val msg = state.actionMessage ?: state.actionError
                        if (msg != null) {
                            snackbarHostState.showSnackbar(msg)
                            viewModel.clearActionMessages()
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Filter & Search Header
                        Surface(
                            shadowElevation = 1.dp,
                            color = PureWhiteSurface,
                            border = BorderStroke(1.dp, BorderDivider)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = state.searchQuery,
                                    onValueChange = viewModel::onSearchQueryChanged,
                                    placeholder = { Text("Search customer or description...", color = TextSecondary, fontSize = 14.sp) },
                                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = EmeraldGreen) },
                                    trailingIcon = {
                                        if (state.searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary)
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = OffWhiteBg,
                                        unfocusedContainerColor = OffWhiteBg,
                                        focusedBorderColor = EmeraldGreen,
                                        unfocusedBorderColor = BorderDivider
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MovementFilterChip(
                                        label = "All",
                                        isSelected = state.selectedTypeFilter == null,
                                        onClick = { viewModel.onTypeFilterChanged(null) }
                                    )
                                    MovementFilterChip(
                                        label = "Debits",
                                        isSelected = state.selectedTypeFilter == MovementType.DEBIT,
                                        onClick = { viewModel.onTypeFilterChanged(MovementType.DEBIT) }
                                    )
                                    MovementFilterChip(
                                        label = "Credits",
                                        isSelected = state.selectedTypeFilter == MovementType.CREDIT,
                                        onClick = { viewModel.onTypeFilterChanged(MovementType.CREDIT) }
                                    )
                                }
                            }
                        }

                        // Multi-selection Action Bar
                        MultiSelectActionBar(
                            selectedCount = state.selectedMovementIds.size,
                            totalCount = state.filteredMovements.count { it.verificationStatus != VerificationStatus.VERIFIED },
                            isDeleting = state.isDeletingBatch,
                            onSelectAll = {
                                val selectableIds = state.filteredMovements
                                    .filter { it.verificationStatus != VerificationStatus.VERIFIED }
                                    .map { it.id }
                                viewModel.selectAllMovements(selectableIds)
                            },
                            onClearSelection = {
                                viewModel.clearMovementSelection()
                            },
                            onDeleteSelected = {
                                showBulkDeleteDialog = true
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )

                        if (state.filteredMovements.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                EmptyStatePlaceholder(
                                    icon = Icons.Outlined.History,
                                    text = "No records found",
                                    subText = "Financial movements will appear here."
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 40.dp)
                            ) {
                                items(state.filteredMovements, key = { it.id }) { movement ->
                                    MovementItemCard(
                                        movement = movement,
                                        onClick = {
                                            navController.navigate(Screen.FinancialMovementDetails.createRoute(movement.id))
                                        },
                                        isSelected = state.selectedMovementIds.contains(movement.id),
                                        isSelectionActive = state.selectedMovementIds.isNotEmpty(),
                                        onToggleSelect = {
                                            viewModel.toggleSelectMovement(movement.id)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (showBulkDeleteDialog) {
                        MultiSelectDeleteConfirmationDialog(
                            itemType = "financial movement",
                            selectedCount = state.selectedMovementIds.size,
                            warningMessage = "የተረጋገጡ የፋይናንስ እንቅስቃሴዎች ለሂሳብ ኦዲት ደህንነት ሲባል ሊሰረዙ አይችሉም። (Verified financial movements are protected and cannot be deleted to preserve accounting audit integrity).",
                            isDeleting = state.isDeletingBatch,
                            onConfirm = {
                                viewModel.bulkDeleteSelectedMovements { _, _, _ ->
                                    showBulkDeleteDialog = false
                                }
                            },
                            onDismiss = {
                                if (!state.isDeletingBatch) {
                                    showBulkDeleteDialog = false
                                }
                            }
                        )
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun MovementFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) EmeraldGreen else PureWhiteSurface,
        border = BorderStroke(1.dp, if (isSelected) EmeraldGreen else BorderDivider)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else TextPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun MovementItemCard(
    movement: FinancialMovement,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    isSelectionActive: Boolean = false,
    onToggleSelect: (() -> Unit)? = null
) {
    val isVerified = movement.verificationStatus == VerificationStatus.VERIFIED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                if (isSelectionActive && onToggleSelect != null && !isVerified) {
                    onToggleSelect()
                } else {
                    onClick()
                }
            }),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) LightMint.copy(alpha = 0.45f) else PureWhiteSurface
        ),
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) EmeraldGreen else BorderDivider
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onToggleSelect != null) {
                if (isVerified) {
                    IconButton(
                        onClick = { onToggleSelect() },
                        modifier = Modifier.size(32.dp).padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Verified (Locked)",
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = EmeraldGreen,
                            uncheckedColor = TextSecondary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = (if (movement.type == MovementType.DEBIT) ErrorColor else EmeraldGreen).copy(alpha = 0.12f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (movement.type == MovementType.DEBIT) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (movement.type == MovementType.DEBIT) ErrorColor else EmeraldGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movement.customerName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = movement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1
                )
                Text(
                    text = movement.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${movement.amount.toInt()} ETB",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (movement.type == MovementType.DEBIT) ErrorColor else EmeraldGreen
                )
                Spacer(modifier = Modifier.height(2.dp))
                VerificationStatusBadge(status = movement.verificationStatus)
            }
        }
    }
}

