package com.chuledigita.mobile.ui.theme

import androidx.compose.ui.graphics.Color

// Web Palette Redesign
val BrandGreen = Color(0xFF173B35)
val WarmOffWhite = Color(0xFFF8F5EE)
val CreamSurface = Color(0xFFFFFDF8)
val BrandGold = Color(0xFFE9BD59)
val BrandBlue = Color(0xFF2C6E91)

// Light Theme Palette
val LightPrimary = BrandGreen
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = BrandGold.copy(alpha = 0.2f)
val LightOnPrimaryContainer = BrandGreen

val LightSecondary = BrandBlue
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = BrandBlue.copy(alpha = 0.1f)
val LightOnSecondaryContainer = BrandBlue

val LightBackground = WarmOffWhite
val LightOnBackground = BrandGreen
val LightSurface = CreamSurface
val LightOnSurface = BrandGreen
val LightSurfaceVariant = Color(0xFFEAE6DA)
val LightOnSurfaceVariant = BrandGreen.copy(alpha = 0.7f)

// Dark Theme Palette (Keep existing for now, but prioritize Light for Login redesign)
val DarkPrimary = BrandGold
val DarkOnPrimary = Color(0xFF000000)
val DarkPrimaryContainer = BrandGreen
val DarkOnPrimaryContainer = BrandGold

val DarkSecondary = BrandBlue
val DarkOnSecondary = Color(0xFFFFFFFF)
val DarkSecondaryContainer = Color(0xFF1B3D4F)
val DarkOnSecondaryContainer = BrandBlue

val DarkBackground = Color(0xFF0F1B18)
val DarkOnBackground = WarmOffWhite
val DarkSurface = Color(0xFF172B26)
val DarkOnSurface = WarmOffWhite
val DarkSurfaceVariant = Color(0xFF233F3A)
val DarkOnSurfaceVariant = WarmOffWhite.copy(alpha = 0.7f)

// Semantic colors
val SuccessColor = Color(0xFF2D6A4F)
val WarningColor = Color(0xFFD4A373)
val ErrorColor = Color(0xFFBC4749)
