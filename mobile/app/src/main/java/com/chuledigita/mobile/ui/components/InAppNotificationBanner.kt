package com.chuledigita.mobile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chuledigita.mobile.domain.model.NotificationItem
import com.chuledigita.mobile.domain.model.NotificationType
import com.chuledigita.mobile.notifications.InAppPopUpState

private val PopUpNavy = Color(0xFF0F172A)
private val PopUpNavyBorder = Color(0xFF1E293B)
private val EmeraldAccent = Color(0xFF10B981)
private val AmberAccent = Color(0xFFF59E0B)
private val IndigoAccent = Color(0xFF6366F1)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextSecondary = Color(0xFF94A3B8)

@Composable
fun InAppNotificationBanner(
    popUpState: InAppPopUpState?,
    onDismiss: () -> Unit,
    onPopUpClick: (InAppPopUpState) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = popUpState != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        if (popUpState != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .statusBarsPadding()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp), spotColor = EmeraldAccent.copy(alpha = 0.25f))
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    EmeraldAccent.copy(alpha = 0.8f),
                                    PopUpNavyBorder,
                                    EmeraldAccent.copy(alpha = 0.3f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .background(PopUpNavy)
                        .clickable { onPopUpClick(popUpState) },
                    color = PopUpNavy,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    when (popUpState) {
                        is InAppPopUpState.Single -> {
                            SingleNotificationContent(
                                item = popUpState.item,
                                onDismiss = onDismiss
                            )
                        }
                        is InAppPopUpState.Summary -> {
                            SummaryNotificationContent(
                                count = popUpState.count,
                                latest = popUpState.latestItem,
                                onDismiss = onDismiss
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleNotificationContent(
    item: NotificationItem,
    onDismiss: () -> Unit
) {
    val (icon, tint) = getIconAndColorForType(item.type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon bubble
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f))
                .border(1.dp, tint.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and message
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (item.time.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${item.time}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.message,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Dismiss action
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SummaryNotificationContent(
    count: Int,
    latest: NotificationItem,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon bubble for aggregated summary
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(EmeraldAccent.copy(alpha = 0.18f))
                .border(1.dp, EmeraldAccent.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = EmeraldAccent,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "$count New Business Updates",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Latest: ${latest.title} — Tap to review all in Notification Center",
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun getIconAndColorForType(type: NotificationType): Pair<ImageVector, Color> {
    return when (type) {
        NotificationType.ORDER_CREATED,
        NotificationType.ORDER_VERIFIED,
        NotificationType.ORDER_REJECTED,
        NotificationType.ORDER_STATUS_UPDATED -> Pair(Icons.Outlined.ShoppingBag, AmberAccent)

        NotificationType.CUSTOMER_NOTE_CREATED,
        NotificationType.BUSINESS_NOTE_CREATED -> Pair(Icons.Outlined.Description, IndigoAccent)

        NotificationType.CUSTOMER_CREATED,
        NotificationType.CUSTOMER_UPDATED,
        NotificationType.CUSTOMER_DELETED,
        NotificationType.CUSTOMER_BULK_DELETED -> Pair(Icons.Outlined.Person, EmeraldAccent)

        NotificationType.FINANCIAL_MOVEMENT_CREATED,
        NotificationType.FINANCIAL_MOVEMENT_VERIFIED,
        NotificationType.FINANCIAL_MOVEMENT_REJECTED,
        NotificationType.FINANCIAL_MOVEMENT_DELETED,
        NotificationType.FINANCIAL_MOVEMENT_BULK_DELETED,
        NotificationType.PAYMENT_RECORDED -> Pair(Icons.Outlined.Payments, EmeraldAccent)

        NotificationType.SYSTEM -> Pair(Icons.Outlined.Notifications, EmeraldAccent)
    }
}
