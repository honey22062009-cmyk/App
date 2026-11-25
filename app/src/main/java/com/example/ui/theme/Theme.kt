package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.dp

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = Color(0xFF25163F),
    primaryContainer = Color(0xFF49336E),
    onPrimaryContainer = Color(0xFFF0E5FF),
    secondary = Color(0xFFFFA2B5),
    onSecondary = Color(0xFF3A1524),
    secondaryContainer = Color(0xFF603143),
    onSecondaryContainer = Color(0xFFFFE0E7),
    tertiary = BrandEmeraldDark,
    onTertiary = Color(0xFF082E25),
    tertiaryContainer = Color(0xFF1B5144),
    onTertiaryContainer = Color(0xFFCDF6E9),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkSurfaceBorder,
    error = Color(0xFFFFB4C1),
    onError = Color(0xFF561D2A),
    errorContainer = Color(0xFF713042),
    onErrorContainer = Color(0xFFFFD9E0)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DDFB),
    onPrimaryContainer = Color(0xFF321B55),
    secondary = Color(0xFFCA5C78),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0E8),
    onSecondaryContainer = Color(0xFF511C2D),
    tertiary = BrandEmerald,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD4F2E9),
    onTertiaryContainer = Color(0xFF123E33),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightSurfaceBorder,
    error = BrandCrimson,
    onError = Color.White,
    errorContainer = Color(0xFFFFE1E7),
    onErrorContainer = Color(0xFF641F32)
)

@Composable
fun StrictBlockerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = StrictBlockerTheme(darkTheme, dynamicColor, content)
