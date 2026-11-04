package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// A calmer lavender-and-rose identity that stays readable in both themes.
val BrandPrimary = Color(0xFF7554C7)
val BrandPrimaryDark = Color(0xFFC7B5FF)

// Supporting status tones avoid the harsh green/blue default palette.
val BrandEmerald = Color(0xFF6279B8)
val BrandEmeraldDark = Color(0xFFB1C2FF)
val BrandAmber = Color(0xFFB77742)
val BrandCrimson = Color(0xFFB64F78)

// Dark surfaces use a soft plum-charcoal base instead of flat black.
val DarkBackground = Color(0xFF121019)
val DarkSurface = Color(0xFF1B1925)
val DarkSurfaceVariant = Color(0xFF262332)
val DarkSurfaceBorder = Color(0xFF393447)
val DarkOnBackground = Color(0xFFF7F4FC)
val DarkOnSurface = Color(0xFFF7F4FC)
val DarkOnSurfaceVariant = Color(0xFFB8B2C4)
val DarkOutline = Color(0xFF555064)

// Light surfaces use a subtle lilac tint with generous contrast.
val LightBackground = Color(0xFFF8F6FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0ECF7)
val LightSurfaceBorder = Color(0xFFE4DDED)
val LightOnBackground = Color(0xFF201C29)
val LightOnSurface = Color(0xFF201C29)
val LightOnSurfaceVariant = Color(0xFF6D6678)
val LightOutline = Color(0xFFBDB5CB)

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
