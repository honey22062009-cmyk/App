package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(CornerRadii.compact),
    small = RoundedCornerShape(CornerRadii.control),
    medium = RoundedCornerShape(CornerRadii.card),
    large = RoundedCornerShape(CornerRadii.dialog),
    extraLarge = RoundedCornerShape(CornerRadii.large)
)

private val DarkColorScheme = darkColorScheme(
    surfaceTint = BrandPrimaryDark,
    surfaceDim = DarkBackground,
    surfaceBright = Color(0xFF33423A),
    surfaceContainerLowest = Color(0xFF0B130F),
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = Color(0xFF2A382F),
    surfaceContainerHighest = Color(0xFF33423A),
    primary = BrandPrimaryDark,
    onPrimary = Color(0xFF103D2C),
    primaryContainer = Color(0xFF2A4F3D),
    onPrimaryContainer = Color(0xFFC7EED8),
    secondary = BrandCoralDark,
    onSecondary = Color(0xFF4C1C16),
    secondaryContainer = Color(0xFF5B302C),
    onSecondaryContainer = Color(0xFFFFDAD4),
    tertiary = BrandEmeraldDark,
    onTertiary = Color(0xFF08372D),
    tertiaryContainer = Color(0xFF154B40),
    onTertiaryContainer = Color(0xFFD5F5EC),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkSurfaceBorder,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    surfaceTint = BrandPrimary,
    surfaceDim = Color(0xFFDCE3D7),
    surfaceBright = LightSurface,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurface,
    surfaceContainer = Color(0xFFF0F2E9),
    surfaceContainerHigh = Color(0xFFE9EDE2),
    surfaceContainerHighest = LightSurfaceVariant,
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9EDDF),
    onPrimaryContainer = Color(0xFF163E2D),
    secondary = BrandCoral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD4),
    onSecondaryContainer = Color(0xFF4A1913),
    tertiary = BrandEmerald,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD9F3EB),
    onTertiaryContainer = Color(0xFF0F4B3F),
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
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun StrictBlockerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

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
