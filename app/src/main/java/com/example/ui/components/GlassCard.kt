package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
 * Kept for source compatibility with the previous glass implementation.
 * The production UI now resolves these values to restrained Material surfaces.
 */
data class GlassSpec(
    val backgroundColor: Color,
    val borderBrush: Brush,
    val specularBrush: Brush?,
    val blurRadius: Dp,
    val borderWidth: Dp
)

object GlassDefaults {
    @Composable
    fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    @Composable
    fun spec(
        variant: GlassVariant = GlassVariant.Standard,
        customBackground: Color? = null,
        customBorder: Color? = null,
        customBlur: Dp? = null,
        borderWidth: Dp = 1.dp
    ): GlassSpec {
        val background = customBackground ?: when (variant) {
            GlassVariant.Standard -> MaterialTheme.colorScheme.surface.copy(alpha = if (isDarkTheme()) 0.66f else 0.74f)
            GlassVariant.Subtle -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDarkTheme()) 0.46f else 0.58f)
            GlassVariant.Elevated -> MaterialTheme.colorScheme.surface.copy(alpha = if (isDarkTheme()) 0.78f else 0.84f)
            GlassVariant.Accent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDarkTheme()) 0.44f else 0.52f)
            GlassVariant.Success -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = if (isDarkTheme()) 0.40f else 0.50f)
            GlassVariant.Warning -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (isDarkTheme()) 0.40f else 0.52f)
            GlassVariant.Error -> MaterialTheme.colorScheme.errorContainer.copy(alpha = if (isDarkTheme()) 0.46f else 0.58f)
        }
        val border = customBorder ?: when (variant) {
            GlassVariant.Accent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
            GlassVariant.Success -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
            GlassVariant.Error -> MaterialTheme.colorScheme.error.copy(alpha = 0.22f)
            else -> MaterialTheme.colorScheme.outlineVariant
        }
        return GlassSpec(
            backgroundColor = background,
            borderBrush = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = if (isDarkTheme()) 0.18f else 0.72f),
                    border.copy(alpha = 0.34f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                )
            ),
            specularBrush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (isDarkTheme()) 0.09f else 0.34f),
                    Color.Transparent,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.035f)
                )
            ),
            blurRadius = customBlur ?: 18.dp,
            borderWidth = borderWidth
        )
    }
}

/**
 * Default production surface. Prefer this component for ordinary content.
 */
@Composable
fun AppSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = backgroundColor,
            tonalElevation = elevation,
            shadowElevation = elevation,
            border = BorderStroke(borderWidth, borderColor)
        ) {
            content()
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = backgroundColor,
            tonalElevation = elevation,
            shadowElevation = elevation,
            border = BorderStroke(borderWidth, borderColor)
        ) {
            content()
        }
    }
}

/**
 * Compatibility wrapper for existing screens. It now renders a restrained,
 * theme-aware translucent surface with a soft specular layer and gradient edge.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
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
    val requested = backgroundColor ?: when (variant) {
        GlassVariant.Accent -> MaterialTheme.colorScheme.primaryContainer
        GlassVariant.Success -> MaterialTheme.colorScheme.tertiaryContainer
        GlassVariant.Warning -> MaterialTheme.colorScheme.secondaryContainer
        GlassVariant.Error -> MaterialTheme.colorScheme.errorContainer
        GlassVariant.Subtle -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> MaterialTheme.colorScheme.surface
    }
    val color = requested.compositeOver(MaterialTheme.colorScheme.surface)
    val border = BorderStroke(borderWidth, borderColor ?: MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color,
            contentColor = MaterialTheme.colorScheme.onSurface, border = border,
            tonalElevation = 0.dp, shadowElevation = elevation, content = content)
    } else {
        Surface(modifier = modifier, shape = shape, color = color,
            contentColor = MaterialTheme.colorScheme.onSurface, border = border,
            tonalElevation = 0.dp, shadowElevation = elevation, content = content)
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    variant: GlassVariant = GlassVariant.Standard,
    borderWidth: Dp = 1.dp,
    borderColor: Color? = null,
    backgroundColor: Color? = null,
    blurRadius: Dp? = null,
    content: @Composable () -> Unit
) = GlassCard(
    modifier = modifier,
    shape = shape,
    variant = variant,
    borderWidth = borderWidth,
    borderColor = borderColor,
    backgroundColor = backgroundColor,
    blurRadius = blurRadius,
    content = content
)

/**
 * Retained for compatibility, but intentionally rendered as quiet text metadata.
 */
@Composable
fun StatusBadge(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showDot: Boolean = false
) {
    val color = if (isActive) activeColor else inactiveColor
    Row(
        modifier = modifier.padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Former atmospheric/glow backdrop. Production UI intentionally uses a neutral canvas.
 */
@Composable
fun AppBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()
    }
}
