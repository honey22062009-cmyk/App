package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Brand: one restrained blue accent across both themes.
val BrandPrimary = Color(0xFF3458D4)
val BrandPrimaryDark = Color(0xFF9CB0FF)

// Semantic colors. Use only for state, never decoration.
val BrandEmerald = Color(0xFF187A67)
val BrandEmeraldDark = Color(0xFF6CC9B5)
val BrandAmber = Color(0xFFB56A00)
val BrandCrimson = Color(0xFFBA1A1A)

// Dark theme: neutral, not cyber-blue.
val DarkBackground = Color(0xFF101114)
val DarkSurface = Color(0xFF181A1E)
val DarkSurfaceVariant = Color(0xFF202227)
val DarkSurfaceBorder = Color(0xFF2B2E34)
val DarkOnBackground = Color(0xFFF3F4F6)
val DarkOnSurface = Color(0xFFF3F4F6)
val DarkOnSurfaceVariant = Color(0xFFA6ABB4)
val DarkOutline = Color(0xFF464A52)

// Light theme: warm-neutral and readable.
val LightBackground = Color(0xFFF7F8FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F2F5)
val LightSurfaceBorder = Color(0xFFE1E4E9)
val LightOnBackground = Color(0xFF17191D)
val LightOnSurface = Color(0xFF17191D)
val LightOnSurfaceVariant = Color(0xFF626871)
val LightOutline = Color(0xFFB8BEC7)

// Compatibility aliases retained for older screens while the UI migrates.
val CyberCyan = BrandPrimary
val SapphireBlue = BrandPrimary
val DarkNavy = DarkSurface
val DeepObsidian = DarkBackground
val CardSurfaceDark = DarkSurface
val CardSurfaceVariantDark = DarkSurfaceVariant
val CrimsonAlert = BrandCrimson
val EmeraldSuccess = BrandEmerald
val AmberWarning = BrandAmber
val SlateGray = DarkOnSurfaceVariant
val LightSlate = LightSurfaceVariant
