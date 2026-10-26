package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.navigation.Screen

@Composable
fun FloatingGlassDock(
    currentScreen: Screen,
    onSelect: (Screen) -> Unit,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 820.dp)
                .fillMaxWidth()
                .shadow(22.dp, shape)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.56f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.72f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.20f)
                        )
                    ),
                    shape
                )
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = 0.30f),
                                Color.White.copy(alpha = 0.07f),
                                Color.Transparent
                            ),
                            center = Offset(180f, 0f),
                            radius = 430f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.055f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 7.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Screen.navItems.forEach { screen ->
                    val selected = currentScreen.route == screen.route
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 1f,
                        animationSpec = if (reduceMotion) {
                            spring(stiffness = 10000f)
                        } else {
                            spring(dampingRatio = 0.70f, stiffness = 520f)
                        },
                        label = "dock_item_scale"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .scale(scale)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onSelect(screen) }
                            .padding(vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(37.dp)
                                .background(
                                    if (selected) {
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFFE95D9B),
                                                Color(0xFFF68A6B),
                                                Color(0xFFA855C7)
                                            )
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            listOf(Color.Transparent, Color.Transparent)
                                        )
                                    },
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title,
                                tint = if (selected) Color.White
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = screen.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
