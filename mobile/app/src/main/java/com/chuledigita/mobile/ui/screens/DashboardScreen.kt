package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.theme.*
import kotlin.math.abs

@Composable
fun DashboardScreen(
    navController: NavHostController,
    onLogout: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MainScaffold(
        navController = navController,
        title = "ዳሽቦርድ",
        onLogout = onLogout,
        showBottomBar = true
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmOffWhite)
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandGreen)
                    }
                }

                is DashboardUiState.Error -> {
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
                            onClick = { viewModel.loadDashboard() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                        ) {
                            Text("እንደገና ሞክር")
                        }
                    }
                }

                is DashboardUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Header
                        item {
                            DashboardHeader()
                        }

                        // 4 Metrics Grid (View-only summary cards)
                        item {
                            DashboardMetricsGrid(state = state)
                        }

                        // Recent Orders Panel (View-only)
                        item {
                            DashboardRecentOrdersSection(orders = state.recentOrders)
                        }

                        // Recent Notes Panel (View-only)
                        item {
                            DashboardRecentNotesSection(notes = state.recentNotes)
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader() {
    Column {
        Text(
            text = "ዳሽቦርድ",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = BrandGreen
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "ማስታወሻዎችን፣ ትዕዛዞችን እና የደንበኞችን ሂሳብ በአንድ ቦታ ይመልከቱ።",
            style = MaterialTheme.typography.bodyMedium,
            color = BrandGreen.copy(alpha = 0.65f),
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun DashboardMetricsGrid(
    state: DashboardUiState.Success
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardMetricCard(
                modifier = Modifier.weight(1f),
                title = "ማስታወሻዎች",
                value = state.notesCount.toString(),
                accentColor = BrandBlue
            )

            DashboardMetricCard(
                modifier = Modifier.weight(1f),
                title = "ትዕዛዞች",
                value = state.ordersCount.toString(),
                accentColor = BrandGreen
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardMetricCard(
                modifier = Modifier.weight(1f),
                title = "ደንበኞች",
                value = state.customersCount.toString(),
                accentColor = BrandGold
            )

            val isNegative = state.currentBalance < 0
            val prefix = if (state.currentBalance > 0) "+ " else if (isNegative) "− " else ""
            val balanceColor = if (isNegative) ErrorColor else SuccessColor
            val formattedBalance = "$prefix${abs(state.currentBalance).toInt()} ብር"

            DashboardMetricCard(
                modifier = Modifier.weight(1f),
                title = "አጠቃላይ ሂሳብ",
                value = formattedBalance,
                valueColor = balanceColor,
                accentColor = balanceColor
            )
        }
    }
}

@Composable
private fun DashboardMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    valueColor: Color = BrandGreen,
    accentColor: Color
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CreamSurface,
        border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.08f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = BrandGreen.copy(alpha = 0.6f),
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DashboardRecentOrdersSection(
    orders: List<DashboardRecentOrder>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CreamSurface,
        border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.08f)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Column {
                Text(
                    text = "የትዕዛዝ ዝርዝር",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "የቅርብ ጊዜ ትዕዛዞች",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (orders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ምንም ትዕዛዝ አልተመዘገበም።",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandGreen.copy(alpha = 0.5f)
                    )
                }
            } else {
                orders.forEachIndexed { index, order ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = order.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = order.date,
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandGreen.copy(alpha = 0.5f)
                            )
                        }
                    }

                    if (index < orders.size - 1) {
                        HorizontalDivider(color = BrandGreen.copy(alpha = 0.06f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardRecentNotesSection(
    notes: List<DashboardRecentNote>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CreamSurface,
        border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.08f)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Column {
                Text(
                    text = "የጋራ ማስታወሻ",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "የቅርብ ጊዜ ማስታወሻዎች",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ምንም ማስታወሻ አልተመዘገበም።",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandGreen.copy(alpha = 0.5f)
                    )
                }
            } else {
                notes.forEachIndexed { index, note ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = note.date,
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandGreen.copy(alpha = 0.5f)
                            )
                        }
                    }

                    if (index < notes.size - 1) {
                        HorizontalDivider(color = BrandGreen.copy(alpha = 0.06f))
                    }
                }
            }
        }
    }
}
