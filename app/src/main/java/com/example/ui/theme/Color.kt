package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette: warm magenta, coral, plum and mint. Deliberately no blue accent.
val BrandPrimary = Color(0xFFB94E88)
val BrandPrimaryDark = Color(0xFFFFA9D0)
val BrandPink = Color(0xFFE35B9A)
val BrandPinkDark = Color(0xFFFFA6CB)
val BrandCoral = Color(0xFFF47B69)
val BrandCoralDark = Color(0xFFFFB3A7)
val BrandPlum = Color(0xFF8E4DB7)
val BrandPlumDark = Color(0xFFD8AEF2)

// Semantic colors.
val BrandEmerald = Color(0xFF187A67)
val BrandEmeraldDark = Color(0xFF72D3BD)
val BrandAmber = Color(0xFFB56A00)
val BrandCrimson = Color(0xFFBA1A1A)

// Dark theme: deep charcoal-plum rather than navy.
val DarkBackground = Color(0xFF100E13)
val DarkSurface = Color(0xFF19161D)
val DarkSurfaceVariant = Color(0xFF241F29)
val DarkSurfaceBorder = Color(0xFF342D3B)
val DarkOnBackground = Color(0xFFF7F1F5)
val DarkOnSurface = Color(0xFFF7F1F5)
val DarkOnSurfaceVariant = Color(0xFFB7AEB7)
val DarkOutline = Color(0xFF514653)

// Light theme: warm pearl with pink-plum neutrals.
val LightBackground = Color(0xFFFBF7F9)
val LightSurface = Color(0xFFFFFBFD)
val LightSurfaceVariant = Color(0xFFF5EDF2)
val LightSurfaceBorder = Color(0xFFE8DDE4)
val LightOnBackground = Color(0xFF1E181D)
val LightOnSurface = Color(0xFF1E181D)
val LightOnSurfaceVariant = Color(0xFF6F636C)
val LightOutline = Color(0xFFC4B7C0)

// Compatibility aliases retained for older screens while the UI migrates.
val CyberCyan = BrandPrimary
val SapphireBlue = BrandPlum
val DarkNavy = DarkSurface
val DeepObsidian = DarkBackground
val CardSurfaceDark = DarkSurface
val CardSurfaceVariantDark = DarkSurfaceVariant
val CrimsonAlert = BrandCrimson
val EmeraldSuccess = BrandEmerald
val AmberWarning = BrandAmber
val SlateGray = DarkOnSurfaceVariant
val LightSlate = LightSurfaceVariant
