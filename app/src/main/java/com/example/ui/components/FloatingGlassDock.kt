package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.navigation.Screen

@Composable
fun FloatingGlassDock(currentScreen: Screen, onSelect: (Screen) -> Unit, reduceMotion: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center) {
        Surface(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(), shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow, shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Row(Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Screen.navItems.forEach { screen ->
                    val selected = currentScreen == screen
                    val scale by animateFloatAsState(if (selected && !reduceMotion) 1.04f else 1f,
                        animationSpec = spring(dampingRatio = .72f, stiffness = 520f), label = "navigation_selection")
                    Column(Modifier.weight(1f).scale(scale).clip(RoundedCornerShape(20.dp))
                        .selectable(selected, role = Role.Tab) { onSelect(screen) }.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(36.dp).background(
                            if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                            CircleShape), contentAlignment = Alignment.Center) {
                            Icon(if (selected) screen.selectedIcon else screen.unselectedIcon, null,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(21.dp))
                        }
                        Text(screen.title, style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
