package com.example.ui.screens.splash

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.BunnyCompanion
import com.example.ui.components.BunnyMood
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    val title = "STRICT BLOCKER"
    var bunnyEntered by remember { mutableStateOf(false) }
    var revealedCharacters by remember { mutableIntStateOf(0) }
    var taglineVisible by remember { mutableStateOf(false) }
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }

    val bunnyOffset by animateDpAsState(
        targetValue = if (bunnyEntered) 0.dp else (-150).dp,
        animationSpec = tween(durationMillis = 540, easing = FastOutSlowInEasing),
        label = "splash_bunny_entry"
    )

    LaunchedEffect(motionEnabled) {
        if (!motionEnabled) {
            bunnyEntered = true
            revealedCharacters = title.length
            taglineVisible = true
            onSplashComplete()
            return@LaunchedEffect
        }
        bunnyEntered = true
        delay(220L)
        for (count in 1..title.length) {
            revealedCharacters = count
            delay(38L)
        }
        delay(100L)
        taglineVisible = true
        delay(360L)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("app_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BunnyCompanion(
                modifier = Modifier.offset(x = bunnyOffset),
                size = 154.dp,
                mood = BunnyMood.Running
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = title.take(revealedCharacters),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold
            )
            AnimatedVisibility(
                visible = taglineVisible,
                enter = fadeIn(tween(220)) +
                    slideInVertically(
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                        initialOffsetY = { it / 3 }
                    ) +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(260))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Make space for what matters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
