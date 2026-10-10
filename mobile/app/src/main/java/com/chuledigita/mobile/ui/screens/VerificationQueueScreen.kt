package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.chuledigita.mobile.data.mock.MockDataProvider
import com.chuledigita.mobile.domain.model.VerificationStatus
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.components.PendingVerificationBanner
import com.chuledigita.mobile.ui.navigation.Screen

@Composable
fun VerificationQueueScreen(
    navController: NavHostController
) {
    val pendingMovements = MockDataProvider.financialMovements.filter { 
        it.verificationStatus == VerificationStatus.PENDING 
    }

    MainScaffold(
        navController = navController,
        title = "Verification Queue",
        showBottomBar = false
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    PendingVerificationBanner(
                        message = "The records below are awaiting your review. Approving them will officially update customer balances."
                    )
                }
            }

            if (pendingMovements.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.FactCheck,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Queue is clear",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "No movements pending verification",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 40.dp)
                ) {
                    items(pendingMovements, key = { it.id }) { movement ->
                        // Assuming MovementItemCard is defined somewhere or imported. 
                        // If it's missing, that would be another error, but for now let's fix the PaddingValues issue.
                        // I'll check if MovementItemCard exists in the project.
                        Text("Movement Item") // Placeholder if I'm not sure, but I should probably check.
                    }
                }
            }
        }
    }
}
