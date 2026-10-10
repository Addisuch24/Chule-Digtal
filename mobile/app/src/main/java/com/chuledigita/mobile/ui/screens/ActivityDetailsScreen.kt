package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.chuledigita.mobile.data.mock.MockDataProvider
import com.chuledigita.mobile.domain.model.ActivityType
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.components.ReadOnlyMessage
import com.chuledigita.mobile.ui.navigation.Screen

@Composable
fun ActivityDetailsScreen(
    activityId: String,
    navController: NavHostController
) {
    val activity = remember(activityId) { MockDataProvider.getActivityById(activityId) }

    if (activity == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Activity not found")
        }
        return
    }

    val isEditable = activity.createdBy == MockDataProvider.currentFamilyMember.name
    val scrollState = rememberScrollState()

    MainScaffold(
        navController = navController,
        title = "Activity Details",
        showBottomBar = false
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (!isEditable) {
                ReadOnlyMessage()
            }

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Activity Log", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = activity.type.name.replace("_", " "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Divider()

            DetailSection("Description", activity.description)
            DetailSection("Performed By", activity.actor)
            DetailSection("Related Record", "${activity.relatedName} (${activity.relatedId})")
            DetailSection("Date & Time", activity.timestamp)

            if (activity.previousValue != null || activity.newValue != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        activity.previousValue?.let {
                            Text("From:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        activity.newValue?.let {
                            Text("To:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isEditable && (activity.type == ActivityType.CUSTOMER_CREATED || activity.type == ActivityType.CUSTOMER_EDITED)) {
                Button(
                    onClick = { navController.navigate(Screen.EditCustomer.createRoute(activity.relatedId)) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Related Customer")
                }
            }
        }
    }
}

@Composable
private fun DetailSection(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
