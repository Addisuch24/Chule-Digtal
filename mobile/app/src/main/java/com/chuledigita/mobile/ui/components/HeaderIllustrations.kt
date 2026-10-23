package com.chuledigita.mobile.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chuledigita.mobile.ui.theme.*

/**
 * High-quality, native Compose Canvas illustrations matching the reference design board.
 * Lightweight, zero-dependency, razor-sharp on all screen densities.
 */

@Composable
fun DashboardHeaderIllustration(
    modifier: Modifier = Modifier.size(90.dp, 80.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Soft background glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(LightMint, Color.Transparent),
                center = Offset(w * 0.6f, h * 0.5f),
                radius = w * 0.5f
            )
        )

        // Bar 1 (Left - Teal)
        val barWidth = w * 0.12f
        val corner = CornerRadius(barWidth / 2f, barWidth / 2f)
        drawRoundRect(
            color = BrandTeal.copy(alpha = 0.85f),
            topLeft = Offset(w * 0.15f, h * 0.55f),
            size = Size(barWidth, h * 0.35f),
            cornerRadius = corner
        )

        // Bar 2 (Middle - Navy)
        drawRoundRect(
            color = BrandNavy.copy(alpha = 0.8f),
            topLeft = Offset(w * 0.35f, h * 0.40f),
            size = Size(barWidth, h * 0.50f),
            cornerRadius = corner
        )

        // Bar 3 (Right - Emerald Green)
        drawRoundRect(
            color = EmeraldGreen,
            topLeft = Offset(w * 0.55f, h * 0.22f),
            size = Size(barWidth, h * 0.68f),
            cornerRadius = corner
        )

        // Rising Trend Arrow Line
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.62f)
            cubicTo(
                w * 0.30f, h * 0.50f,
                w * 0.45f, h * 0.35f,
                w * 0.72f, h * 0.16f
            )
        }
        drawPath(
            path = path,
            color = EmeraldGreen,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

        // Arrow head
        val arrowPath = Path().apply {
            moveTo(w * 0.78f, h * 0.12f)
            lineTo(w * 0.65f, h * 0.12f)
            lineTo(w * 0.76f, h * 0.24f)
            close()
        }
        drawPath(
            path = arrowPath,
            color = EmeraldGreen,
            style = Fill
        )

        // Sparkle Accent
        drawCircle(
            color = WarningColor,
            radius = 3.dp.toPx(),
            center = Offset(w * 0.28f, h * 0.22f)
        )
        drawCircle(
            color = EmeraldGreen.copy(alpha = 0.6f),
            radius = 2.dp.toPx(),
            center = Offset(w * 0.82f, h * 0.36f)
        )
    }
}

@Composable
fun NotesHeaderIllustration(
    modifier: Modifier = Modifier.size(90.dp, 80.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Soft glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(LightMint, Color.Transparent),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.55f
            )
        )

        // Notepad body (creamy-yellow with rounded corners)
        val padLeft = w * 0.20f
        val padTop = h * 0.16f
        val padWidth = w * 0.50f
        val padHeight = h * 0.70f
        val corner = CornerRadius(8.dp.toPx(), 8.dp.toPx())

        // Pad shadow
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.08f),
            topLeft = Offset(padLeft + 3.dp.toPx(), padTop + 4.dp.toPx()),
            size = Size(padWidth, padHeight),
            cornerRadius = corner
        )

        // Main Notepad
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7))
            ),
            topLeft = Offset(padLeft, padTop),
            size = Size(padWidth, padHeight),
            cornerRadius = corner
        )
        drawRoundRect(
            color = Color(0xFFF59E0B).copy(alpha = 0.3f),
            topLeft = Offset(padLeft, padTop),
            size = Size(padWidth, padHeight),
            cornerRadius = corner,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Top spiral/clips
        for (i in 0..3) {
            val cx = padLeft + (i + 1) * (padWidth / 5f)
            drawRoundRect(
                color = Color(0xFFD97706),
                topLeft = Offset(cx - 2.dp.toPx(), padTop - 4.dp.toPx()),
                size = Size(4.dp.toPx(), 8.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }

        // Horizontal ruled lines on pad
        for (i in 1..4) {
            val lineY = padTop + padHeight * (0.22f + i * 0.15f)
            drawLine(
                color = Color(0xFFD97706).copy(alpha = 0.25f),
                start = Offset(padLeft + padWidth * 0.15f, lineY),
                end = Offset(padLeft + padWidth * 0.85f, lineY),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Tilted Pencil/Stylus leaning on the right
        val penStart = Offset(w * 0.62f, h * 0.15f)
        val penEnd = Offset(w * 0.78f, h * 0.75f)
        drawLine(
            color = EmeraldGreen,
            start = penStart,
            end = penEnd,
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Pen tip
        drawLine(
            color = BrandNavy,
            start = Offset(w * 0.78f, h * 0.75f),
            end = Offset(w * 0.82f, h * 0.85f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun OrdersHeaderIllustration(
    modifier: Modifier = Modifier.size(90.dp, 80.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Soft glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(LightMint, Color.Transparent),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.55f
            )
        )

        // Main Box 1 (Left / Back - Orange Parcel)
        val box1Left = w * 0.18f
        val box1Top = h * 0.38f
        val box1Size = w * 0.32f
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFDBA74), Color(0xFFFB923C))
            ),
            topLeft = Offset(box1Left, box1Top),
            size = Size(box1Size, box1Size),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        // Tape on box 1
        drawLine(
            color = Color(0xFFEA580C).copy(alpha = 0.6f),
            start = Offset(box1Left + box1Size * 0.5f, box1Top),
            end = Offset(box1Left + box1Size * 0.5f, box1Top + box1Size),
            strokeWidth = 3.dp.toPx()
        )

        // Box 2 (Stacked Front - Emerald Gift Box)
        val box2Left = w * 0.32f
        val box2Top = h * 0.22f
        val box2W = w * 0.28f
        val box2H = h * 0.26f
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(EmeraldGreen, Color(0xFF0D7A54))
            ),
            topLeft = Offset(box2Left, box2Top),
            size = Size(box2W, box2H),
            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
        )
        // Tape / Ribbon
        drawLine(
            color = LightMint,
            start = Offset(box2Left + box2W * 0.5f, box2Top),
            end = Offset(box2Left + box2W * 0.5f, box2Top + box2H),
            strokeWidth = 2.dp.toPx()
        )

        // Shopping Cart Outline (Teal / Navy)
        val cartColor = BrandNavy
        val cartPath = Path().apply {
            moveTo(w * 0.58f, h * 0.32f)
            lineTo(w * 0.65f, h * 0.32f)
            lineTo(w * 0.70f, h * 0.58f)
            lineTo(w * 0.88f, h * 0.58f)
            lineTo(w * 0.92f, h * 0.36f)
            lineTo(w * 0.66f, h * 0.36f)
        }
        drawPath(
            path = cartPath,
            color = cartColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Cart Wheels
        drawCircle(color = cartColor, radius = 3.dp.toPx(), center = Offset(w * 0.73f, h * 0.66f))
        drawCircle(color = cartColor, radius = 3.dp.toPx(), center = Offset(w * 0.85f, h * 0.66f))
    }
}

@Composable
fun FinanceHeaderIllustration(
    modifier: Modifier = Modifier.size(90.dp, 80.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Soft glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(LightMint, Color.Transparent),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.55f
            )
        )

        // Wallet / Card holder (Navy)
        val walletLeft = w * 0.38f
        val walletTop = h * 0.28f
        val walletW = w * 0.52f
        val walletH = h * 0.48f
        val walletCorner = CornerRadius(8.dp.toPx(), 8.dp.toPx())

        // Wallet shadow
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.08f),
            topLeft = Offset(walletLeft + 2.dp.toPx(), walletTop + 3.dp.toPx()),
            size = Size(walletW, walletH),
            cornerRadius = walletCorner
        )

        // Wallet body
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(BrandNavy, Color(0xFF0F2236))
            ),
            topLeft = Offset(walletLeft, walletTop),
            size = Size(walletW, walletH),
            cornerRadius = walletCorner
        )

        // Card sticking out (Emerald)
        drawRoundRect(
            color = EmeraldGreen,
            topLeft = Offset(walletLeft + walletW * 0.15f, walletTop - h * 0.08f),
            size = Size(walletW * 0.70f, h * 0.14f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Wallet clasp flap
        val claspW = walletW * 0.28f
        val claspH = walletH * 0.35f
        drawRoundRect(
            color = BrandTeal,
            topLeft = Offset(walletLeft + walletW - claspW * 0.7f, walletTop + walletH * 0.32f),
            size = Size(claspW, claspH),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        drawCircle(
            color = WarningColor,
            radius = 2.5.dp.toPx(),
            center = Offset(walletLeft + walletW - claspW * 0.2f, walletTop + walletH * 0.50f)
        )

        // Gold Coins Stack (Left)
        val coinRadius = w * 0.14f
        val coinCenters = listOf(
            Offset(w * 0.24f, h * 0.65f),
            Offset(w * 0.24f, h * 0.53f),
            Offset(w * 0.24f, h * 0.41f)
        )
        coinCenters.forEachIndexed { i, center ->
            // Coin base / 3D edge
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                ),
                topLeft = Offset(center.x - coinRadius, center.y - 6.dp.toPx()),
                size = Size(coinRadius * 2, 12.dp.toPx())
            )
            // Coin top face
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFDE68A), Color(0xFFFBBF24))
                ),
                topLeft = Offset(center.x - coinRadius, center.y - 9.dp.toPx()),
                size = Size(coinRadius * 2, 10.dp.toPx())
            )
            // Inner rim
            drawOval(
                color = Color(0xFFD97706).copy(alpha = 0.5f),
                topLeft = Offset(center.x - coinRadius * 0.7f, center.y - 7.dp.toPx()),
                size = Size(coinRadius * 1.4f, 6.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
fun CustomersHeaderIllustration(
    modifier: Modifier = Modifier.size(90.dp, 80.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Soft glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(LightMint, Color.Transparent),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.55f
            )
        )

        // Customer 1 (Left - Emerald)
        val c1x = w * 0.32f
        val c1y = h * 0.32f
        val headRadius = w * 0.14f
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(EmeraldGreen, Color(0xFF0D7A54))
            ),
            radius = headRadius,
            center = Offset(c1x, c1y)
        )
        // Shoulder / Body
        val c1Body = Path().apply {
            moveTo(c1x - headRadius * 1.6f, h * 0.85f)
            cubicTo(
                c1x - headRadius * 1.4f, h * 0.55f,
                c1x + headRadius * 1.4f, h * 0.55f,
                c1x + headRadius * 1.6f, h * 0.85f
            )
            close()
        }
        drawPath(path = c1Body, brush = Brush.verticalGradient(listOf(EmeraldGreen, Color(0xFF0D7A54))))

        // Customer 2 (Right / Overlapping - Navy/Teal)
        val c2x = w * 0.65f
        val c2y = h * 0.38f
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(BrandNavy, BrandTeal)
            ),
            radius = headRadius * 0.95f,
            center = Offset(c2x, c2y)
        )
        val c2Body = Path().apply {
            moveTo(c2x - headRadius * 1.5f, h * 0.88f)
            cubicTo(
                c2x - headRadius * 1.3f, h * 0.60f,
                c2x + headRadius * 1.3f, h * 0.60f,
                c2x + headRadius * 1.5f, h * 0.88f
            )
            close()
        }
        drawPath(path = c2Body, brush = Brush.verticalGradient(listOf(BrandNavy, BrandTeal)))

        // Connection / Sparkle indicator
        drawCircle(
            color = WarningColor,
            radius = 3.dp.toPx(),
            center = Offset(w * 0.50f, h * 0.20f)
        )
    }
}

@Composable
fun NotificationsHeaderIllustration(
    modifier: Modifier = Modifier.size(90.dp, 80.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Soft glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(LightMint, Color.Transparent),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.55f
            )
        )

        // 3D Bell Body (Deep Teal / Navy)
        val bellPath = Path().apply {
            moveTo(w * 0.50f, h * 0.18f)
            cubicTo(w * 0.40f, h * 0.25f, w * 0.30f, h * 0.50f, w * 0.22f, h * 0.64f)
            lineTo(w * 0.78f, h * 0.64f)
            cubicTo(w * 0.70f, h * 0.50f, w * 0.60f, h * 0.25f, w * 0.50f, h * 0.18f)
            close()
        }
        drawPath(
            path = bellPath,
            brush = Brush.verticalGradient(
                colors = listOf(BrandTeal, BrandNavy)
            )
        )

        // Bell base rim
        drawRoundRect(
            color = BrandNavy,
            topLeft = Offset(w * 0.18f, h * 0.62f),
            size = Size(w * 0.64f, 6.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )

        // Bell Clapper
        drawCircle(
            color = WarningColor,
            radius = 5.dp.toPx(),
            center = Offset(w * 0.50f, h * 0.74f)
        )

        // Active Red Notification Badge
        drawCircle(
            color = ErrorColor,
            radius = 8.dp.toPx(),
            center = Offset(w * 0.74f, h * 0.22f)
        )
        drawCircle(
            color = Color.White,
            radius = 7.dp.toPx(),
            center = Offset(w * 0.74f, h * 0.22f),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

/**
 * Reusable Screen Header matching the curved wave design on the reference board.
 */
@Composable
fun ScreenHeaderBanner(
    title: String,
    subtitle: String? = null,
    illustration: @Composable (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        LightMint.copy(alpha = 0.85f),
                        OffWhiteBg
                    )
                )
            )
            .padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy,
                    letterSpacing = (-0.5).sp
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (illustration != null) {
                    illustration()
                }
                if (action != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    action()
                }
            }
        }
    }
}

/**
 * Background corner fluid wave for auth screens (Login & SignUp)
 */
@Composable
fun AuthBackgroundWaves(modifier: Modifier = Modifier.fillMaxSize()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Top-right fluid wave (mint & teal)
        val topWave = Path().apply {
            moveTo(w * 0.35f, 0f)
            cubicTo(
                w * 0.45f, h * 0.12f,
                w * 0.75f, h * 0.08f,
                w, h * 0.22f
            )
            lineTo(w, 0f)
            close()
        }
        drawPath(
            path = topWave,
            brush = Brush.linearGradient(
                colors = listOf(LightMint.copy(alpha = 0.7f), Color(0xFFD1F2E2).copy(alpha = 0.5f))
            )
        )

        // Top-left soft secondary wave
        val topLeftWave = Path().apply {
            moveTo(0f, 0f)
            cubicTo(
                w * 0.20f, h * 0.06f,
                w * 0.30f, h * 0.14f,
                0f, h * 0.24f
            )
            close()
        }
        drawPath(
            path = topLeftWave,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFE2F7EE).copy(alpha = 0.6f), Color.Transparent)
            )
        )

        // Bottom flowing wave
        val bottomWave = Path().apply {
            moveTo(0f, h * 0.88f)
            cubicTo(
                w * 0.30f, h * 0.94f,
                w * 0.60f, h * 0.82f,
                w, h * 0.90f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = bottomWave,
            brush = Brush.verticalGradient(
                colors = listOf(LightMint.copy(alpha = 0.5f), Color(0xFFD6F5E6).copy(alpha = 0.7f))
            )
        )
    }
}

/**
 * Top wave background for Profile screen matching reference board
 */
@Composable
fun ProfileHeaderWave(modifier: Modifier = Modifier.fillMaxWidth().height(180.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Background main gradient curve
        val mainPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h * 0.72f)
            cubicTo(
                w * 0.75f, h * 0.98f,
                w * 0.35f, h * 0.50f,
                0f, h * 0.85f
            )
            close()
        }
        drawPath(
            path = mainPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    EmeraldGreen,
                    BrandTeal,
                    Color(0xFF0D7A54)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        // Accent lighter wave overlay
        val accentPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h * 0.45f)
            cubicTo(
                w * 0.70f, h * 0.75f,
                w * 0.40f, h * 0.30f,
                0f, h * 0.60f
            )
            close()
        }
        drawPath(
            path = accentPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.15f),
                    Color.Transparent
                )
            )
        )
    }
}

