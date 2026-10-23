package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.delay
import kotlin.math.abs

import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import com.chuledigita.mobile.ui.components.DashboardHeaderIllustration
import com.chuledigita.mobile.ui.components.ScreenHeaderBanner
import com.chuledigita.mobile.ui.navigation.Screen
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun DashboardScreen(
    navController: NavHostController,
    onLogout: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        viewModel.loadDashboard()
        onPauseOrDispose { }
    }

    MainScaffold(
        navController = navController,
        title = "ዳሽቦርድ",
        onLogout = onLogout,
        showBottomBar = true
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OffWhiteBg)
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldGreen)
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
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("እንደገና ሞክር")
                        }
                    }
                }

                is DashboardUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Banner matching Reference Board
                        item {
                            ScreenHeaderBanner(
                                title = "Dashboard",
                                subtitle = "Manage your business together.\nየቤተሰብ ንግድን አብረው ያስተዳድሩ።",
                                illustration = {
                                    DashboardHeaderIllustration()
                                },
                                action = {
                                    IconButton(
                                        onClick = { viewModel.loadDashboard() },
                                        modifier = Modifier
                                            .background(PureWhiteSurface, CircleShape)
                                            .border(1.dp, BorderDivider, CircleShape)
                                            .size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh Dashboard",
                                            tint = BrandNavy,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )
                        }

                        // 4 Primary Metric Cards in 2x2 Grid (Notes, Orders, Customers, Balance)
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    DashboardNavMetricCard(
                                        modifier = Modifier.weight(1f),
                                        title = "Notes",
                                        amharicTitle = "ማስታወሻዎች",
                                        value = state.notesCount.toString(),
                                        icon = Icons.Outlined.Description,
                                        iconTint = EmeraldGreen,
                                        onViewAllClick = { navController.navigate(Screen.Notes.route) }
                                    )

                                    DashboardNavMetricCard(
                                        modifier = Modifier.weight(1f),
                                        title = "Orders",
                                        amharicTitle = "ትዕዛዞች",
                                        value = state.ordersCount.toString(),
                                        icon = Icons.Outlined.ShoppingBag,
                                        iconTint = BrandTeal,
                                        onViewAllClick = { navController.navigate(Screen.Orders.route) }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    DashboardNavMetricCard(
                                        modifier = Modifier.weight(1f),
                                        title = "Customers",
                                        amharicTitle = "ደንበኞች",
                                        value = state.customersCount.toString(),
                                        icon = Icons.Outlined.People,
                                        iconTint = SlateBlue,
                                        onViewAllClick = { navController.navigate(Screen.Customers.route) }
                                    )

                                    val isNegative = state.currentBalance < 0
                                    val prefix = if (state.currentBalance > 0) "+ " else if (isNegative) "− " else ""
                                    val balanceColor = if (isNegative) ErrorColor else EmeraldGreen
                                    val formattedBalance =
                                        ("$prefix${abs(state.currentBalance).toInt()} ETB").also {

                                            DashboardNavMetricCard(
                                                Modifier.weight(1f),
                                                "Current Balance",
                                                "አጠቃላይ ሂሳብ",
                                                it,
                                                balanceColor,
                                                Icons.Outlined.Payment,
                                                balanceColor,
                                                { navController.navigate(Screen.Finance.route) },
                                                "View details"
                                            )
                                        }
                                }

                                // Secondary Debit / Credit Accounting Cards
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val formattedDebit = "− ${abs(state.totalDebit).toInt()} ETB"
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        color = PureWhiteSurface,
                                        border = BorderStroke(1.dp, BorderDivider),
                                        shadowElevation = 1.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = ErrorColor.copy(alpha = 0.1f),
                                                shape = CircleShape,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                                        contentDescription = null,
                                                        tint = ErrorColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "ጠቅላላ ዕዳ (Debit)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                                Text(
                                                    text = formattedDebit,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ErrorColor
                                                )
                                            }
                                        }
                                    }

                                    val formattedCredit = "+ ${abs(state.totalCredit).toInt()} ETB"
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        color = PureWhiteSurface,
                                        border = BorderStroke(1.dp, BorderDivider),
                                        shadowElevation = 1.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = EmeraldGreen.copy(alpha = 0.1f),
                                                shape = CircleShape,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                                        contentDescription = null,
                                                        tint = EmeraldGreen,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "ጠቅላላ ክሬዲት",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                                Text(
                                                    text = formattedCredit,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldGreen
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Brand Card with Subtle Typewriter
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                AnimatedChuleDigitalCard()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardNavMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    amharicTitle: String,
    value: String,
    valueColor: Color = BrandNavy,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color = EmeraldGreen,
    onViewAllClick: () -> Unit,
    actionText: String = "View all"
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = PureWhiteSurface,
        border = BorderStroke(1.dp, BorderDivider),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = LightMint,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandNavy
                    )
                    Text(
                        text = amharicTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            ) {
                TextButton(
                    onClick = onViewAllClick,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text(
                        text = "$actionText →",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                }
            }
        }
    }
}


@Composable
private fun AnimatedChuleDigitalCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CreamSurface,
        border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.08f)),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "FAMILY BUSINESS NETWORK",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandGold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                AnimatedTypewriterText(fullText = "Chule Digital")
            }
        }
    }
}

@Composable
fun AnimatedTypewriterText(
    fullText: String,
    modifier: Modifier = Modifier
) {
    var displayedText by remember { mutableStateOf("") }

    LaunchedEffect(fullText) {
        while (true) {
            // Type out letter by letter
            for (i in 1..fullText.length) {
                displayedText = fullText.take(i)
                delay(120.milliseconds)
            }
            delay(1800.milliseconds) // pause at full text
            // Erase letter by letter
            for (i in fullText.length downTo 0) {
                displayedText = fullText.take(i)
                delay(70.milliseconds)
            }
            delay(400.milliseconds) // pause before repeating
        }
    }

    Text(
        text = displayedText.ifEmpty { " " },
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.ExtraBold,
        color = BrandGreen,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}
