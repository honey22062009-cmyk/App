package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class BunnyMood { Resting, Focused, Celebrating, Running }

/**
 * Original study companion made from native Compose shapes.
 * Subtle breathing is continuous; blinks, ear twitches and paw gestures
 * happen at irregular intervals without requiring user interaction.
 */
@Composable
fun BunnyCompanion(
    modifier: Modifier = Modifier,
    size: Dp = 176.dp,
    mood: BunnyMood = BunnyMood.Resting
) {
    val fur = Color(0xFFFFF8EA)
    val furShade = Color(0xFFE9DDC8)
    val earInner = Color(0xFFE9AFA2)
    val ink = Color(0xFF3F4038)
    val blush = Color(0xFFE8B5A9)
    val scarf = Color(0xFF7651C8)
    val book = Color(0xFF49336E)

    var blinking by remember { mutableStateOf(false) }
    var earTwitch by remember { mutableStateOf(false) }
    var pawWave by remember { mutableStateOf(false) }
    var curiousTilt by remember { mutableStateOf(false) }

    val breathing = rememberInfiniteTransition(label = "bunny_breathing")
    val breath by breathing.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bunny_breath"
    )
    val earAngle by animateFloatAsState(
        targetValue = if (earTwitch) 12f else 0f,
        animationSpec = tween(220),
        label = "bunny_ear_twitch"
    )
    val pawAngle by animateFloatAsState(
        targetValue = if (pawWave) -25f else 8f,
        animationSpec = tween(260),
        label = "bunny_paw_wave"
    )
    val bodyTilt by animateFloatAsState(
        targetValue = if (curiousTilt) -5f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "bunny_body_tilt"
    )

    LaunchedEffect(mood) {
        if (mood == BunnyMood.Running) {
            while (true) {
                pawWave = true
                earTwitch = true
                delay(145L)
                pawWave = false
                earTwitch = false
                delay(145L)
            }
        } else {
            while (true) {
                delay(Random.nextLong(2_800L, 6_200L))
                blinking = true
                delay(130L)
                blinking = false

                if (Random.nextFloat() < 0.38f) {
                    earTwitch = true
                    delay(460L)
                    earTwitch = false
                }

                if (mood == BunnyMood.Celebrating || Random.nextFloat() < 0.32f) {
                    pawWave = true
                    delay(520L)
                    pawWave = false
                }

                if (Random.nextFloat() < 0.42f) {
                    curiousTilt = true
                    earTwitch = true
                    delay(480L)
                    curiousTilt = false
                    earTwitch = false
                }
            }
        }
    }

    val movementY = when (mood) {
        BunnyMood.Running -> if (pawWave) -size * 0.045f else size * 0.012f
        BunnyMood.Celebrating -> (-3.5f + breath * 5f).dp
        else -> (-breath * 2f).dp
    }
    val eyeSize = if (mood == BunnyMood.Focused) size * 0.038f else size * 0.046f
    val eyeHeight = if (blinking) size * 0.012f else eyeSize

    Box(
        modifier = modifier
            .size(size)
            .semantics {
                contentDescription = when (mood) {
                    BunnyMood.Resting -> "Study bunny, resting"
                    BunnyMood.Focused -> "Study bunny, focusing"
                    BunnyMood.Celebrating -> "Study bunny, celebrating"
                    BunnyMood.Running -> "Study bunny, running"
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.92f)
                .offset(y = size * 0.035f)
                .clip(CircleShape)
                .background(Color(0xFFE6EBDC))
        )
        Box(
            modifier = Modifier
                .size(size * 0.56f, size * 0.09f)
                .align(Alignment.BottomCenter)
                .offset(y = -size * 0.035f)
                .clip(CircleShape)
                .background(Color(0xFFCAD2C3))
        )

        Box(
            modifier = Modifier
                .size(size)
                .offset(y = movementY)
                .graphicsLayer {
                    rotationY = bodyTilt
                    rotationZ = bodyTilt * 0.22f
                    cameraDistance = 12f * density
                },
            contentAlignment = Alignment.Center
        ) {
            BunnyPart(
                Modifier.align(Alignment.TopCenter)
                    .offset(x = -size * 0.17f, y = size * 0.035f)
                    .rotate(-14f - earAngle)
                    .size(size * 0.22f, size * 0.53f),
                furShade, RoundedCornerShape(50)
            )
            BunnyPart(
                Modifier.align(Alignment.TopCenter)
                    .offset(x = size * 0.16f, y = size * 0.025f)
                    .rotate(12f + earAngle * 0.45f)
                    .size(size * 0.22f, size * 0.52f),
                furShade, RoundedCornerShape(50)
            )
            BunnyPart(
                Modifier.align(Alignment.TopCenter)
                    .offset(x = -size * 0.17f, y = size * 0.065f)
                    .rotate(-14f - earAngle)
                    .size(size * 0.13f, size * 0.40f),
                earInner, RoundedCornerShape(50)
            )
            BunnyPart(
                Modifier.align(Alignment.TopCenter)
                    .offset(x = size * 0.16f, y = size * 0.055f)
                    .rotate(12f + earAngle * 0.45f)
                    .size(size * 0.13f, size * 0.39f),
                earInner, RoundedCornerShape(50)
            )

            BunnyPart(
                Modifier.align(Alignment.BottomCenter)
                    .offset(x = -size * 0.13f, y = -size * 0.015f)
                    .size(size * 0.27f, size * 0.16f),
                furShade, RoundedCornerShape(50)
            )
            BunnyPart(
                Modifier.align(Alignment.BottomCenter)
                    .offset(x = size * 0.13f, y = -size * 0.015f)
                    .size(size * 0.27f, size * 0.16f),
                furShade, RoundedCornerShape(50)
            )
            BunnyPart(
                Modifier.align(Alignment.BottomCenter)
                    .offset(y = -size * 0.055f)
                    .size(size * 0.58f, size * 0.48f),
                fur, RoundedCornerShape(46)
            )
            BunnyPart(
                Modifier.align(Alignment.BottomCenter)
                    .offset(y = -size * 0.12f)
                    .size(size * 0.34f, size * 0.29f),
                Color(0xFFFFFDF7), RoundedCornerShape(50)
            )

            BunnyPart(
                Modifier.align(Alignment.CenterStart)
                    .offset(x = size * 0.19f, y = size * 0.20f)
                    .rotate(18f)
                    .size(size * 0.18f, size * 0.31f),
                fur, RoundedCornerShape(50)
            )
            BunnyPart(
                Modifier.align(Alignment.CenterEnd)
                    .offset(x = -size * 0.19f, y = size * 0.20f)
                    .rotate(pawAngle)
                    .size(size * 0.18f, size * 0.31f),
                fur, RoundedCornerShape(50)
            )
            BunnyPart(
                Modifier.align(Alignment.CenterEnd)
                    .offset(x = -size * 0.22f, y = size * 0.32f)
                    .rotate(pawAngle)
                    .size(size * 0.14f),
                furShade, CircleShape
            )

            BunnyPart(
                Modifier.align(Alignment.TopCenter)
                    .offset(y = size * 0.20f)
                    .size(size * 0.71f, size * 0.61f),
                fur, RoundedCornerShape(48)
            )
            BunnyPart(
                Modifier.align(Alignment.TopCenter)
                    .offset(x = -size * 0.18f, y = size * 0.50f)
                    .size(size * 0.14f),
                blush.copy(alpha = 0.84f), CircleShape
            )
            BunnyPart(
                Modifier.align(Alignment.TopCenter)
                    .offset(x = size * 0.18f, y = size * 0.50f)
                    .size(size * 0.14f),
                blush.copy(alpha = 0.84f), CircleShape
            )
            BunnyPart(
                Modifier.align(Alignment.TopCenter)
                    .offset(x = -size * 0.14f, y = size * 0.455f)
                    .size(eyeSize, eyeHeight),
                ink, CircleShape
            )
            BunnyPart(
                Modifier.align(Alignment.TopCenter)
                    .offset(x = size * 0.14f, y = size * 0.455f)
                    .size(eyeSize, eyeHeight),
                ink, CircleShape
            )
            BunnyPart(
                Modifier.align(Alignment.TopCenter)
                    .offset(y = size * 0.535f)
                    .size(size * 0.06f, size * 0.045f),
                earInner, RoundedCornerShape(50)
            )
            BunnyPart(
                Modifier.align(Alignment.TopCenter)
                    .offset(y = size * 0.572f)
                    .size(size * 0.035f),
                ink.copy(alpha = 0.8f), CircleShape
            )

            BunnyPart(
                Modifier.align(Alignment.Center)
                    .offset(y = size * 0.305f)
                    .size(size * 0.63f, size * 0.085f)
                    .rotate(-4f),
                scarf, RoundedCornerShape(50)
            )
            BunnyPart(
                Modifier.align(Alignment.BottomCenter)
                    .offset(x = size * 0.16f, y = -size * 0.16f)
                    .size(size * 0.25f, size * 0.29f)
                    .rotate(5f),
                book, RoundedCornerShape(7.dp)
            )
            BunnyPart(
                Modifier.align(Alignment.BottomCenter)
                    .offset(x = size * 0.16f, y = -size * 0.16f)
                    .size(size * 0.19f, size * 0.23f)
                    .rotate(5f),
                Color(0xFFEBE1C9), RoundedCornerShape(3.dp)
            )
            BunnyPart(
                Modifier.align(Alignment.BottomCenter)
                    .offset(x = size * 0.16f, y = -size * 0.16f)
                    .size(size * 0.035f, size * 0.18f),
                book, RoundedCornerShape(2.dp)
            )
        }
    }
}

@Composable
private fun BunnyPart(
    modifier: Modifier,
    color: Color,
    shape: androidx.compose.ui.graphics.Shape
) {
    Box(
        modifier = modifier
            .shadow(elevation = 2.5.dp, shape = shape, clip = false)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.36f),
                        color,
                        color.copy(alpha = 0.84f)
                    )
                )
            )
    )
}
