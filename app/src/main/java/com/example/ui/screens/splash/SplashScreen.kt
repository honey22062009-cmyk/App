package com.example.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    val logoScale = remember { Animatable(0.72f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoRotation = remember { Animatable(-8f) }
    val ringScale = remember { Animatable(0.72f) }
    val ringAlpha = remember { Animatable(0f) }
    val copyAlpha = remember { Animatable(0f) }
    val copyOffset = remember { Animatable(10f) }
    val exitAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { logoAlpha.animateTo(1f, tween(180)) }
            launch { logoScale.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
            launch { logoRotation.animateTo(0f, tween(420, easing = FastOutSlowInEasing)) }
            launch { ringAlpha.animateTo(1f, tween(220)) }
            launch { ringScale.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
        }
        coroutineScope {
            launch { copyAlpha.animateTo(1f, tween(220)) }
            launch { copyOffset.animateTo(0f, tween(300, easing = FastOutSlowInEasing)) }
        }
        delay(260)
        exitAlpha.animateTo(0f, tween(160))
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .graphicsLayer { alpha = exitAlpha.value }
            .testTag("app_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .graphicsLayer {
                            scaleX = ringScale.value
                            scaleY = ringScale.value
                            alpha = ringAlpha.value
                        }
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.46f))
                )
                Image(
                    painter = painterResource(R.drawable.app_icon_18_blocker_1792376066440),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            alpha = logoAlpha.value
                            rotationZ = logoRotation.value
                        }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = copyAlpha.value
                    translationY = copyOffset.value
                }
            ) {
                Text(
                    text = "Strict Blocker",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Focus without the noise",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
