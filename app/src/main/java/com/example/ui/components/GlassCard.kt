package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Variants for Theme-Aware Glassmorphism components.
 */
enum class GlassVariant {
    Standard,
    Subtle,
    Elevated,
    Accent,
    Success,
    Warning,
    Error
}

/**
 * Configuration specification for a glass surface computed according to active theme.
 */
data class GlassSpec(
    val backgroundColor: Color,
    val borderBrush: Brush,
    val specularBrush: Brush?,
    val blurRadius: Dp,
    val borderWidth: Dp
)

/**
 * Theme-aware glassmorphism calculation engine.
 * Dynamically adjusts surface opacity, gradient borders, and blur intensities
 * between Light Mode and Dark Mode for optimal contrast, legibility, and restraint.
 */
object GlassDefaults {

    @Composable
    fun isDarkTheme(): Boolean {
        val systemDark = isSystemInDarkTheme()
        val surfaceLuminance = MaterialTheme.colorScheme.surface.luminance()
        return systemDark || surfaceLuminance < 0.5f
    }

    @Composable
    fun spec(
        variant: GlassVariant = GlassVariant.Standard,
        customBackground: Color? = null,
        customBorder: Color? = null,
        customBlur: Dp? = null,
        borderWidth: Dp = 1.dp
    ): GlassSpec {
        val isDark = isDarkTheme()
        val defaultBlur = customBlur ?: if (isDark) 18.dp else 16.dp

        // If caller passed a custom background, use it directly or with appropriate glass opacity
        val baseBackground = customBackground ?: when (variant) {
            GlassVariant.Standard -> {
                if (isDark) Color(0xFF111827).copy(alpha = 0.78f)
                else Color(0xFFFFFFFF).copy(alpha = 0.86f)
            }
            GlassVariant.Subtle -> {
                if (isDark) Color(0xFF111827).copy(alpha = 0.55f)
                else Color(0xFFFFFFFF).copy(alpha = 0.65f)
            }
            GlassVariant.Elevated -> {
                if (isDark) Color(0xFF1E293B).copy(alpha = 0.88f)
                else Color(0xFFFFFFFF).copy(alpha = 0.94f)
            }
            GlassVariant.Accent -> {
                val primary = MaterialTheme.colorScheme.primary
                if (isDark) primary.copy(alpha = 0.16f)
                else primary.copy(alpha = 0.12f)
            }
            GlassVariant.Success -> {
                val emerald = MaterialTheme.colorScheme.tertiary
                if (isDark) emerald.copy(alpha = 0.16f)
                else emerald.copy(alpha = 0.12f)
            }
            GlassVariant.Warning -> {
                val amber = Color(0xFFF59E0B)
                if (isDark) amber.copy(alpha = 0.16f)
                else amber.copy(alpha = 0.12f)
            }
            GlassVariant.Error -> {
                val crimson = MaterialTheme.colorScheme.error
                if (isDark) crimson.copy(alpha = 0.16f)
                else crimson.copy(alpha = 0.12f)
            }
        }

        // Specular highlight brush (subtle reflection along top edge)
        val specularBrush = when {
            isDark -> Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.09f),
                    Color.White.copy(alpha = 0.02f),
                    Color.Transparent
                )
            )
            else -> Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.55f),
                    Color.White.copy(alpha = 0.15f),
                    Color.Transparent
                )
            )
        }

        // Specular gradient border brush
        val borderBrush = if (customBorder != null) {
            Brush.verticalGradient(
                colors = listOf(
                    customBorder.copy(alpha = 0.60f),
                    customBorder.copy(alpha = 0.30f)
                )
            )
        } else {
            when (variant) {
                GlassVariant.Standard, GlassVariant.Subtle, GlassVariant.Elevated -> {
                    if (isDark) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.16f),
                                Color(0xFF334155).copy(alpha = 0.45f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.85f),
                                Color(0xFFCBD5E1).copy(alpha = 0.45f)
                            )
                        )
                    }
                }
                GlassVariant.Accent -> {
                    val primary = MaterialTheme.colorScheme.primary
                    Brush.verticalGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.50f),
                            primary.copy(alpha = 0.20f)
                        )
                    )
                }
                GlassVariant.Success -> {
                    val emerald = MaterialTheme.colorScheme.tertiary
                    Brush.verticalGradient(
                        colors = listOf(
                            emerald.copy(alpha = 0.50f),
                            emerald.copy(alpha = 0.20f)
                        )
                    )
                }
                GlassVariant.Warning -> {
                    val amber = Color(0xFFF59E0B)
                    Brush.verticalGradient(
                        colors = listOf(
                            amber.copy(alpha = 0.50f),
                            amber.copy(alpha = 0.20f)
                        )
                    )
                }
                GlassVariant.Error -> {
                    val crimson = MaterialTheme.colorScheme.error
                    Brush.verticalGradient(
                        colors = listOf(
                            crimson.copy(alpha = 0.50f),
                            crimson.copy(alpha = 0.20f)
                        )
                    )
                }
            }
        }

        return GlassSpec(
            backgroundColor = baseBackground,
            borderBrush = borderBrush,
            specularBrush = specularBrush,
            blurRadius = defaultBlur,
            borderWidth = borderWidth
        )
    }
}

/**
 * Production-grade theme-aware Glassmorphism Card.
 * Seamlessly adapts opacity and specular edge gradients between light and dark themes.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    variant: GlassVariant = GlassVariant.Standard,
    borderWidth: Dp = 1.dp,
    borderColor: Color? = null,
    backgroundColor: Color? = null,
    blurRadius: Dp? = null,
    specularHighlight: Boolean = true,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val spec = GlassDefaults.spec(
        variant = variant,
        customBackground = backgroundColor,
        customBorder = borderColor,
        customBlur = blurRadius,
        borderWidth = borderWidth
    )

    val clickableModifier = if (onClick != null) {
        modifier
            .clip(shape)
            .clickable { onClick() }
    } else {
        modifier.clip(shape)
    }

    Box(
        modifier = clickableModifier
            .border(
                width = spec.borderWidth,
                brush = spec.borderBrush,
                shape = shape
            )
            .background(
                color = spec.backgroundColor,
                shape = shape
            )
    ) {
        // Specular top highlight overlay
        if (specularHighlight && spec.specularBrush != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(spec.specularBrush, shape = shape)
            )
        }

        content()
    }
}

/**
 * Flexible container surface with theme-aware glass styling.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    variant: GlassVariant = GlassVariant.Standard,
    borderWidth: Dp = 1.dp,
    borderColor: Color? = null,
    backgroundColor: Color? = null,
    blurRadius: Dp? = null,
    content: @Composable () -> Unit
) {
    GlassCard(
        modifier = modifier,
        shape = shape,
        variant = variant,
        borderWidth = borderWidth,
        borderColor = borderColor,
        backgroundColor = backgroundColor,
        blurRadius = blurRadius,
        content = content
    )
}

/**
 * Refined status pill badge with frosted glass styling and status dot indicator.
 */
@Composable
fun StatusBadge(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF10B981),
    inactiveColor: Color = Color(0xFFEF4444),
    showDot: Boolean = true
) {
    val isDark = GlassDefaults.isDarkTheme()
    val color = if (isActive) activeColor else inactiveColor
    val bgAlpha = if (isDark) 0.16f else 0.12f
    val borderAlpha = if (isDark) 0.35f else 0.28f

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = bgAlpha),
        border = BorderStroke(0.8.dp, color.copy(alpha = borderAlpha))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = color
            )
        }
    }
}

/**
 * Compact theme-aware glass information tag.
 */
@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val isDark = GlassDefaults.isDarkTheme()
    val bgAlpha = if (isDark) 0.16f else 0.10f
    val borderAlpha = if (isDark) 0.30f else 0.22f

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = bgAlpha),
        border = BorderStroke(0.5.dp, color.copy(alpha = borderAlpha))
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}


/**
 * App-wide atmospheric background used behind all primary screens.
 * Keeps the interface visually rich without sacrificing readability.
 */
@Composable
fun AppBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = GlassDefaults.isDarkTheme()
    val base = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(base)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minSide = size.minDimension
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = if (isDark) 0.20f else 0.13f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.14f, size.height * 0.08f),
                    radius = minSide * 0.72f
                ),
                radius = minSide * 0.72f,
                center = Offset(size.width * 0.14f, size.height * 0.08f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondary.copy(alpha = if (isDark) 0.14f else 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.92f, size.height * 0.28f),
                    radius = minSide * 0.58f
                ),
                radius = minSide * 0.58f,
                center = Offset(size.width * 0.92f, size.height * 0.28f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tertiary.copy(alpha = if (isDark) 0.10f else 0.07f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.55f, size.height * 0.96f),
                    radius = minSide * 0.66f
                ),
                radius = minSide * 0.66f,
                center = Offset(size.width * 0.55f, size.height * 0.96f)
            )
        }

        content()
    }
}
