package com.chuledigita.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chuledigita.mobile.domain.model.*
import com.chuledigita.mobile.ui.theme.ErrorColor
import com.chuledigita.mobile.ui.theme.SuccessColor
import com.chuledigita.mobile.ui.theme.WarningColor

@Composable
fun GenericStatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier
    ) {
        Text(
            text = text.uppercase(),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val color = when (status.lowercase()) {
        "completed", "verified", "payment", "success", "confirmed", "ready", "credit" -> SuccessColor
        "pending", "warning", "processing" -> WarningColor
        "cancelled", "rejected", "error", "debit" -> ErrorColor
        else -> MaterialTheme.colorScheme.outline
    }
    GenericStatusBadge(text = status, color = color, modifier = modifier)
}

@Composable
fun OrderStatusBadge(status: OrderStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        OrderStatus.DRAFT -> MaterialTheme.colorScheme.outline
        OrderStatus.PENDING -> WarningColor
        OrderStatus.CONFIRMED -> MaterialTheme.colorScheme.primary
        OrderStatus.PROCESSING -> MaterialTheme.colorScheme.secondary
        OrderStatus.READY -> Color(0xFF00BCD4)
        OrderStatus.COMPLETED -> SuccessColor
        OrderStatus.CANCELLED -> ErrorColor
    }
    GenericStatusBadge(text = status.name, color = color, modifier = modifier)
}

@Composable
fun VerificationStatusBadge(status: VerificationStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        VerificationStatus.NOT_REQUIRED -> MaterialTheme.colorScheme.outline
        VerificationStatus.PENDING -> WarningColor
        VerificationStatus.VERIFIED -> SuccessColor
        VerificationStatus.REJECTED -> ErrorColor
    }
    GenericStatusBadge(text = status.name.replace("_", " "), color = color, modifier = modifier)
}

@Composable
fun MovementTypeBadge(type: MovementType, modifier: Modifier = Modifier) {
    val color = when (type) {
        MovementType.DEBIT -> ErrorColor
        MovementType.CREDIT -> SuccessColor
    }
    GenericStatusBadge(text = type.name, color = color, modifier = modifier)
}

@Composable
fun PaymentMethodBadge(method: PaymentMethod?, modifier: Modifier = Modifier) {
    if (method == null) return
    GenericStatusBadge(text = method.name.replace("_", " "), color = MaterialTheme.colorScheme.tertiary, modifier = modifier)
}

@Composable
fun PendingVerificationBanner(
    message: String = "This transaction is pending admin verification and has not yet affected the official verified balance.",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = WarningColor.copy(alpha = 0.08f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, WarningColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Pending Verification Warning",
                tint = WarningColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = WarningColor,
                lineHeight = 16.sp
            )
        }
    }
}
