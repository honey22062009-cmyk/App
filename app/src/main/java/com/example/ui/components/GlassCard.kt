package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
            GlassVariant.Standard -> MaterialTheme.colorScheme.surface
            GlassVariant.Subtle -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            GlassVariant.Elevated -> MaterialTheme.colorScheme.surface
            GlassVariant.Accent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            GlassVariant.Success -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f)
            GlassVariant.Warning -> MaterialTheme.colorScheme.surfaceVariant
            GlassVariant.Error -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
        }
        val border = customBorder ?: when (variant) {
            GlassVariant.Accent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
            GlassVariant.Success -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
            GlassVariant.Error -> MaterialTheme.colorScheme.error.copy(alpha = 0.22f)
            else -> MaterialTheme.colorScheme.outlineVariant
        }
        return GlassSpec(
            backgroundColor = background,
            borderBrush = Brush.linearGradient(listOf(border, border)),
            specularBrush = null,
            blurRadius = customBlur ?: 0.dp,
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
    Surface(
        modifier = modifier,
        shape = shape,
        color = backgroundColor,
        tonalElevation = elevation,
        shadowElevation = elevation,
        border = BorderStroke(borderWidth, borderColor),
        onClick = onClick ?: {},
        enabled = onClick != null
    ) {
        content()
    }
}

/**
 * Compatibility wrapper for existing screens. The old glass effects were deliberately
 * removed: no blur, specular shine, gradient border, or brand-tinted shadow.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    variant: GlassVariant = GlassVariant.Standard,
    borderWidth: Dp = 1.dp,
    borderColor: Color? = null,
    backgroundColor: Color? = null,
    blurRadius: Dp? = null,
    specularHighlight: Boolean = false,
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

    val base = modifier
        .clip(shape)
        .border(spec.borderWidth, borderColor ?: MaterialTheme.colorScheme.outlineVariant, shape)
        .background(spec.backgroundColor, shape)

    Box(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base
    ) {
        content()
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
