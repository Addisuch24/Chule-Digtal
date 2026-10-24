package com.chuledigita.mobile.ui.theme

import androidx.compose.ui.graphics.Color

// Professional Business Identity Palette matching Reference Board
val BrandNavy = Color(0xFF17324D)
val SlateBlue = Color(0xFF355C7D)
val EmeraldGreen = Color(0xFF15966A)
val BrandTeal = Color(0xFF0F8B8D)
val LightMint = Color(0xFFE7F7F0)
val OffWhiteBg = Color(0xFFF5F7FA)
val PureWhiteSurface = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF172B3A)
val TextSecondary = Color(0xFF64748B)
val BorderDivider = Color(0xFFE2E8F0)

// Semantic colors
val SuccessColor = Color(0xFF15966A)
val WarningColor = Color(0xFFD99A26)
val ErrorColor = Color(0xFFD94A4A)
val InfoColor = Color(0xFF0F8B8D)

// Customer Balance — Balance-Based Color Branding (Web + Android consistent)
val BalancePositiveBlue = Color(0xFF2878D4)      // Primary blue for positive balances (+)
val BalancePositiveBg = Color(0xFFEAF3FF)        // Light blue background for positive balance cards
val BalanceNegativeRed = Color(0xFFDC2626)       // Primary red for negative balances (−)
val BalanceNegativeBg = Color(0xFFFEF2F2)        // Light red background for negative balance cards
val BalanceZeroBg = Color(0xFFFFFFFF)            // White background for zero balances (0)
val BalanceZeroBorder = Color(0xFFE2E8F0)        // Neutral border for zero balances
val BalanceDarkText = Color(0xFF172B43)          // Dark text with sufficient contrast
val BalanceSettledText = Color(0xFF475569)       // Settled status label text
val BalanceSettledBg = Color(0xFFF8FAFC)         // Settled status badge background

// Backward-compatibility aliases
val BrandGreen = EmeraldGreen
val WarmOffWhite = OffWhiteBg
val CreamSurface = PureWhiteSurface
val BrandGold = WarningColor
val BrandBlue = BrandTeal

// Light Theme Palette
val LightPrimary = EmeraldGreen
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = LightMint
val LightOnPrimaryContainer = EmeraldGreen

val LightSecondary = BrandNavy
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = BrandNavy.copy(alpha = 0.08f)
val LightOnSecondaryContainer = BrandNavy

val LightBackground = OffWhiteBg
val LightOnBackground = TextPrimary
val LightSurface = PureWhiteSurface
val LightOnSurface = TextPrimary
val LightSurfaceVariant = PureWhiteSurface
val LightOnSurfaceVariant = TextSecondary

// Dark Theme Palette
val DarkPrimary = EmeraldGreen
val DarkOnPrimary = Color(0xFF000000)
val DarkPrimaryContainer = Color(0xFF0B3A2C)
val DarkOnPrimaryContainer = LightMint

val DarkSecondary = SlateBlue
val DarkOnSecondary = Color(0xFFFFFFFF)
val DarkSecondaryContainer = Color(0xFF1B3D4F)
val DarkOnSecondaryContainer = SlateBlue

val DarkBackground = Color(0xFF0D1821)
val DarkOnBackground = OffWhiteBg
val DarkSurface = Color(0xFF172836)
val DarkOnSurface = OffWhiteBg
val DarkSurfaceVariant = Color(0xFF23384A)
val DarkOnSurfaceVariant = OffWhiteBg.copy(alpha = 0.7f)
