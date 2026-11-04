package com.example.ui.components
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GlassVariant { Standard, Subtle, Elevated, Accent, Success, Warning, Error }
data class GlassSpec(val backgroundColor: Color, val borderBrush: Brush, val specularBrush: Brush?, val blurRadius: Dp, val borderWidth: Dp)

object GlassDefaults {
    @Composable fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    @Composable fun spec(variant: GlassVariant = GlassVariant.Standard, customBackground: Color? = null,
        customBorder: Color? = null, customBlur: Dp? = null, borderWidth: Dp = 1.dp): GlassSpec {
        val dark = isDarkTheme()
        val background = customBackground ?: when (variant) {
            GlassVariant.Standard -> MaterialTheme.colorScheme.surface.copy(alpha = if (dark) .76f else .78f)
            GlassVariant.Subtle -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (dark) .58f else .64f)
            GlassVariant.Elevated -> MaterialTheme.colorScheme.surface.copy(alpha = if (dark) .88f else .92f)
            GlassVariant.Accent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = .68f)
            GlassVariant.Success -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .62f)
            GlassVariant.Warning -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .66f)
            GlassVariant.Error -> MaterialTheme.colorScheme.errorContainer.copy(alpha = .68f)
        }
        val border = customBorder ?: when (variant) {
            GlassVariant.Accent -> MaterialTheme.colorScheme.primary.copy(alpha = .32f)
            GlassVariant.Success -> MaterialTheme.colorScheme.tertiary.copy(alpha = .28f)
            GlassVariant.Error -> MaterialTheme.colorScheme.error.copy(alpha = .30f)
            else -> MaterialTheme.colorScheme.outlineVariant
        }
        return GlassSpec(background, Brush.linearGradient(listOf(Color.White.copy(alpha = if (dark) .30f else .88f),
            border.copy(alpha = .62f), MaterialTheme.colorScheme.primary.copy(alpha = .18f))),
            Brush.verticalGradient(listOf(Color.White.copy(alpha = if (dark) .14f else .42f),
                Color.Transparent, MaterialTheme.colorScheme.primary.copy(alpha = .045f))), customBlur ?: 18.dp, borderWidth)
    }
}
@Composable
fun AppSurface(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(14.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface, borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    borderWidth: Dp = 1.dp, elevation: Dp = 0.dp, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    if (onClick != null) Surface(onClick = onClick, modifier = modifier, shape = shape, color = backgroundColor,
        tonalElevation = elevation, shadowElevation = elevation, border = BorderStroke(borderWidth, borderColor), content = content)
    else Surface(modifier = modifier, shape = shape, color = backgroundColor, tonalElevation = elevation,
        shadowElevation = elevation, border = BorderStroke(borderWidth, borderColor), content = content)
}
/** Soft translucent cards with a fine luminous edge and a restrained top reflection. */
@Composable
fun GlassCard(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(24.dp),
    variant: GlassVariant = GlassVariant.Standard, borderWidth: Dp = 1.dp, borderColor: Color? = null,
    backgroundColor: Color? = null, blurRadius: Dp? = null, specularHighlight: Boolean = true,
    elevation: Dp = 0.dp, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val spec = GlassDefaults.spec(variant, backgroundColor, borderColor, blurRadius, borderWidth)
    val base = spec.backgroundColor.compositeOver(MaterialTheme.colorScheme.background)
    val border = BorderStroke(spec.borderWidth, spec.borderBrush)
    val reflection = if (specularHighlight) spec.specularBrush else null
    val inner: @Composable () -> Unit = {
        if (reflection != null) Box(Modifier.background(reflection)) { content() } else content()
    }
    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.985f else 1f,
            animationSpec = tween(durationMillis = 110),
            label = "glass_card_press"
        )
        Surface(
            onClick = onClick,
            modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
            interactionSource = interactionSource,
            shape = shape,
            color = base,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = border,
            tonalElevation = 0.dp,
            shadowElevation = elevation,
            content = { inner() }
        )
    }
    else Surface(modifier = modifier, shape = shape, color = base, contentColor = MaterialTheme.colorScheme.onSurface,
        border = border, tonalElevation = 0.dp, shadowElevation = elevation, content = { inner() })
}
@Composable
fun GlassSurface(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(14.dp),
    variant: GlassVariant = GlassVariant.Standard, borderWidth: Dp = 1.dp, borderColor: Color? = null,
    backgroundColor: Color? = null, blurRadius: Dp? = null, content: @Composable () -> Unit) =
    GlassCard(modifier, shape, variant, borderWidth, borderColor, backgroundColor, blurRadius, content = content)
@Composable
fun StatusBadge(text: String, isActive: Boolean, modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary, inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showDot: Boolean = false) {
    val color = if (isActive) activeColor else inactiveColor
    Row(modifier.padding(horizontal = 2.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showDot) { Box(Modifier.size(6.dp).clip(CircleShape).background(color)); Spacer(Modifier.width(6.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = color)
    }
}
@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium,
            color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}
@Composable
fun AppBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background,
        MaterialTheme.colorScheme.surface.copy(alpha = .72f), MaterialTheme.colorScheme.background)))) { content() }
}
