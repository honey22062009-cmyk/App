package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.navigation.Screen

/** A translucent, layered dock. Content really remains visible through its surface. */
@Composable
fun FloatingGlassDock(currentScreen: Screen, onSelect: (Screen) -> Unit, reduceMotion: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val items = Screen.navItems
    val selectedIndex = items.indexOf(currentScreen).takeIf { it >= 0 }
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center) {
        val shape = RoundedCornerShape(30.dp)
        Box(modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth().testTag("glass_dock")
            .drawBehind {
                // A painted halo avoids Android elevation shadows bleeding through transparent children.
                for (spread in 8 downTo 1) {
                    val radius = spread.dp.toPx()
                    drawRoundRect(Color.Black.copy(alpha = .006f), Offset(-radius, -radius / 2),
                        Size(size.width + radius * 2, size.height + radius), CornerRadius(30.dp.toPx() + radius))
                }
            }
            .clip(shape)
            .background(colors.surface.copy(alpha = .58f))
            .border(BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = .85f),
                colors.primary.copy(alpha = .17f), Color.White.copy(alpha = .46f)))), shape)) {
            BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(listOf(Color.White.copy(alpha = .12f), colors.primaryContainer.copy(alpha = .12f), Color.Transparent)))
                .padding(6.dp)) {
                val cellWidth = maxWidth / items.size
                val offset by animateDpAsState(cellWidth * (selectedIndex ?: 0),
                    animationSpec = if (reduceMotion) snap() else spring(dampingRatio = .78f, stiffness = 430f), label = "dock_slide")
                if (selectedIndex != null) Box(
                    Modifier.offset(x = offset).width(cellWidth).height(62.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(colors.primaryContainer.copy(alpha = .90f), colors.secondaryContainer.copy(alpha = .78f))))
                        .drawWithCache {
                            val shine = Brush.verticalGradient(listOf(Color.White.copy(alpha = .48f), Color.Transparent))
                            onDrawBehind { drawRect(shine) }
                        }
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    items.forEach { screen ->
                        val selected = currentScreen == screen
                        val tint by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant, label = "dock_tint")
                        Column(Modifier.weight(1f).heightIn(min = 62.dp).clip(RoundedCornerShape(24.dp))
                            .selectable(selected, role = Role.Tab) { if (!selected) onSelect(screen) }
                            .padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(if (selected) screen.selectedIcon else screen.unselectedIcon, null, tint = tint, modifier = Modifier.size(22.dp))
                            Text(screen.title, style = MaterialTheme.typography.labelMedium, maxLines = 1,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = tint)
                        }
                    }
                }
            }
        }
    }
}
