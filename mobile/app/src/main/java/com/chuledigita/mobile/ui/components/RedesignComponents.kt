package com.chuledigita.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chuledigita.mobile.domain.model.*

@Composable
fun CustomerCard(
    customer: Customer,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onPayCredit: () -> Unit,
    onCollectBalance: () -> Unit,
    isEditable: Boolean
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = customer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    customer.phone?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (isEditable) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Icon(
                        Icons.Default.Lock, 
                        contentDescription = "Read-only", 
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (customer.creditStatus != null || customer.remainingBalanceStatus != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    customer.creditStatus?.let { status ->
                        CreditStatusSection(
                            amount = customer.creditAmount,
                            status = status,
                            onPayClick = onPayCredit
                        )
                    }

                    customer.remainingBalanceStatus?.let { status ->
                        RemainingBalanceSection(
                            amount = customer.remainingBalanceAmount,
                            status = status,
                            onCollectClick = onCollectBalance
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "የተመዘገበው በ: ${customer.createdBy}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun CreditStatusSection(
    amount: Double,
    status: CreditStatus,
    onPayClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("የዕዳ ሂሳብ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${amount.toInt()} ETB", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            
            val isPaid = status == CreditStatus.PAID_CREDIT
            Button(
                onClick = onPayClick,
                enabled = !isPaid,
                colors = if (isPaid) 
                    ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f), contentColor = Color(0xFF4CAF50))
                else 
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isPaid) "ከፍሏል" else "ተበድሮታል", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RemainingBalanceSection(
    amount: Double,
    status: RemainingBalanceStatus,
    onCollectClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("በንግዱ ያለ ቀሪ ሂሳብ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${amount.toInt()} ETB", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            
            val isCollected = status == RemainingBalanceStatus.BALANCE_COLLECTED
            Button(
                onClick = onCollectClick,
                enabled = !isCollected,
                colors = if (isCollected)
                    ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f), contentColor = Color(0xFF4CAF50))
                else
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isCollected) "ተቀብሏል" else "ቀሪ ሂሳብ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ActionHistoryItem(action: ActionHistory) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (icon, color) = getActionIconAndColor(action.actionType)
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.1f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = getActionTitle(action.actionType),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "በ: ", style = MaterialTheme.typography.bodySmall)
                Text(text = action.actorFamilyMemberName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "ቀን: ${action.date}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "ሰዓት: ${action.time}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "መግለጫ: ${action.actionDescription}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (action.amount != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "መጠን: ${action.amount.toInt()} ብር",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (action.previousStatus != null || action.newStatus != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    action.previousStatus?.let {
                        Text(text = "ቀድሞ ሁኔታ: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    action.newStatus?.let {
                        Text(text = "አዲስ ሁኔታ: $it", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    }
                }
            }
        }
    }
}

@Composable
private fun getActionIconAndColor(type: ActivityType): Pair<ImageVector, Color> {
    return when (type) {
        ActivityType.CUSTOMER_CREATED -> Icons.Default.PersonAdd to Color(0xFF2196F3)
        ActivityType.CUSTOMER_EDITED -> Icons.Default.Edit to Color(0xFFFF9800)
        ActivityType.CUSTOMER_NOTE_ADDED -> Icons.Default.NoteAdd to Color(0xFF9C27B0)
        ActivityType.CREDIT_RECORDED -> Icons.Default.TrendingUp to Color(0xFFF44336)
        ActivityType.CREDIT_PAID -> Icons.Default.Payments to Color(0xFF4CAF50)
        ActivityType.BALANCE_RECORDED -> Icons.Default.AccountBalanceWallet to Color(0xFF3F51B5)
        ActivityType.BALANCE_COLLECTED -> Icons.Default.DoneAll to Color(0xFF4CAF50)
        else -> Icons.Default.Info to Color.Gray
    }
}

private fun getActionTitle(type: ActivityType): String {
    return when (type) {
        ActivityType.CUSTOMER_CREATED -> "ደንበኛ ተፈጥሯል"
        ActivityType.CUSTOMER_EDITED -> "መረጃ ተስተካክሏል"
        ActivityType.CUSTOMER_NOTE_ADDED -> "ማስታወሻ ተጨምሯል"
        ActivityType.CREDIT_RECORDED -> "ዕዳ ተመዝግቧል"
        ActivityType.CREDIT_PAID -> "ዕዳ ተከፍሏል"
        ActivityType.BALANCE_RECORDED -> "ቀሪ ሂሳብ ተመዝግቧል"
        ActivityType.BALANCE_COLLECTED -> "ቀሪ ሂሳብ ተሰጥቷል"
        else -> "እንቅስቃሴ"
    }
}

@Composable
fun CreditPaymentDialog(
    customerName: String,
    creditAmount: Double,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("የዕዳ ክፍያ ማረጋገጫ") },
        text = {
            Column {
                Text("ደንበኛ: $customerName")
                Spacer(modifier = Modifier.height(8.dp))
                Text("ያለበት ዕዳ: ${creditAmount.toInt()} ETB", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text("ሙሉ ክፍያ መፈጸሙን ያረጋግጣሉ?")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                Text("ክፍያውን አረጋግጥ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ሰርዝ")
            }
        }
    )
}

@Composable
fun RemainingBalanceCollectionDialog(
    customerName: String,
    balanceAmount: Double,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("የቀሪ ሂሳብ ክፍያ ማረጋገጫ") },
        text = {
            Column {
                Text("ደንበኛ: $customerName")
                Spacer(modifier = Modifier.height(8.dp))
                Text("ቀሪ ሂሳብ: ${balanceAmount.toInt()} ETB", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text("ደንበኛው ቀሪ ሂሳቡን መቀበሉን ያረጋግጣሉ?")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                Text("መቀበሉን አረጋግጥ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ሰርዝ")
            }
        }
    )
}

@Composable
fun OrderCard(
    order: Order,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = order.orderNumber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OrderStatusBadge(order.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = order.customerName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = order.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "By: ${order.takenByFamilyMember}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = order.orderDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun ActivityItem(
    activity: Activity,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        headlineContent = { Text(activity.relatedName, fontWeight = FontWeight.Bold) },
        supportingContent = { 
            Column {
                Text(activity.description)
                Text(activity.timestamp, style = MaterialTheme.typography.labelSmall)
            }
        },
        overlineContent = { Text(activity.actor.uppercase(), color = MaterialTheme.colorScheme.primary) },
        leadingContent = {
            val (icon, color) = when (activity.type) {
                ActivityType.CUSTOMER_CREATED -> Icons.Default.Info to MaterialTheme.colorScheme.primary
                ActivityType.ORDER_CREATED -> Icons.Default.Info to MaterialTheme.colorScheme.secondary
                else -> Icons.Default.Info to MaterialTheme.colorScheme.tertiary
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
        },
        trailingContent = {
            TextButton(onClick = onClick) {
                Text("Details")
            }
        }
    )
}

@Composable
fun ReadOnlyMessage(
    message: String = "You can only edit records created by you."
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
