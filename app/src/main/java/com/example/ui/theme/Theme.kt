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
    surfaceBright = Color(0xFF44384B),
    surfaceContainerLowest = Color(0xFF141018),
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = Color(0xFF3C3142),
    surfaceContainerHighest = Color(0xFF44384B),
    primary = BrandPrimaryDark,
    onPrimary = Color(0xFF552344),
    primaryContainer = Color(0xFF663450),
    onPrimaryContainer = Color(0xFFFFD8EB),
    secondary = BrandCoralDark,
    onSecondary = Color(0xFF5B281F),
    secondaryContainer = Color(0xFF713C31),
    onSecondaryContainer = Color(0xFFFFDAD4),
    tertiary = BrandEmeraldDark,
    onTertiary = Color(0xFF222F62),
    tertiaryContainer = Color(0xFF3D477C),
    onTertiaryContainer = Color(0xFFDEE4FF),
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
    surfaceDim = Color(0xFFECE0E9),
    surfaceBright = LightSurface,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurface,
    surfaceContainer = Color(0xFFFCF0F7),
    surfaceContainerHigh = Color(0xFFF9E9F3),
    surfaceContainerHighest = LightSurfaceVariant,
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFAD9EA),
    onPrimaryContainer = Color(0xFF5D2649),
    secondary = BrandCoral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD4),
    onSecondaryContainer = Color(0xFF4A1913),
    tertiary = BrandEmerald,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE3E7FF),
    onTertiaryContainer = Color(0xFF354378),
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
    accentStyle: String = "rose",
    content: @Composable () -> Unit
) {
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val scheme = when (accentStyle) {
        "lilac" -> baseScheme.copy(
            primary = if (darkTheme) BrandPlumDark else BrandPlum,
            onPrimary = if (darkTheme) Color(0xFF342350) else Color.White,
            primaryContainer = if (darkTheme) Color(0xFF4B3A64) else Color(0xFFEEE1FF),
            onPrimaryContainer = if (darkTheme) Color(0xFFF0E3FF) else Color(0xFF46335E))
        "peach" -> baseScheme.copy(
            primary = if (darkTheme) BrandCoralDark else BrandCoral,
            onPrimary = if (darkTheme) Color(0xFF572B22) else Color.White,
            primaryContainer = if (darkTheme) Color(0xFF663C33) else Color(0xFFFFE0D5),
            onPrimaryContainer = if (darkTheme) Color(0xFFFFDFD2) else Color(0xFF64382D))
        else -> baseScheme
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
) = StrictBlockerTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
