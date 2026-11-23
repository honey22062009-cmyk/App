package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class BunnyMood { Resting, Focused, Celebrating }

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
    val scarf = Color(0xFF637A5C)
    val book = Color(0xFF3E5542)

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size * 0.92f)
                .offset(y = size * 0.03f)
                .clip(CircleShape)
                .background(Color(0xFFE6EBDC))
        )

        BunnyPart(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = -size * 0.17f, y = size * 0.045f)
                .rotate(-14f)
                .size(size * 0.23f, size * 0.52f),
            color = furShade,
            shape = RoundedCornerShape(50)
        )
        BunnyPart(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = size * 0.16f, y = size * 0.035f)
                .rotate(12f)
                .size(size * 0.23f, size * 0.52f),
            color = furShade,
            shape = RoundedCornerShape(50)
        )
        BunnyPart(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = -size * 0.17f, y = size * 0.065f)
                .rotate(-14f)
                .size(size * 0.15f, size * 0.40f),
            color = earInner,
            shape = RoundedCornerShape(50)
        )
        BunnyPart(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = size * 0.16f, y = size * 0.055f)
                .rotate(12f)
                .size(size * 0.15f, size * 0.40f),
            color = earInner,
            shape = RoundedCornerShape(50)
        )

        BunnyPart(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -size * 0.02f)
                .size(size * 0.60f, size * 0.48f),
            color = fur,
            shape = RoundedCornerShape(46)
        )
        BunnyPart(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = size * 0.20f)
                .size(size * 0.69f, size * 0.58f),
            color = fur,
            shape = RoundedCornerShape(48)
        )

        // Soft muzzle and cheeks
        BunnyPart(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = -size * 0.16f, y = size * 0.51f)
                .size(size * 0.15f),
            color = blush,
            shape = CircleShape
        )
        BunnyPart(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = size * 0.16f, y = size * 0.51f)
                .size(size * 0.15f),
            color = blush,
            shape = CircleShape
        )

        val eyeY = if (mood == BunnyMood.Focused) size * 0.49f else size * 0.46f
        val eyeSize = if (mood == BunnyMood.Focused) size * 0.035f else size * 0.045f
        BunnyPart(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = -size * 0.14f, y = eyeY)
                .size(eyeSize),
            color = ink,
            shape = CircleShape
        )
        BunnyPart(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = size * 0.14f, y = eyeY)
                .size(eyeSize),
            color = ink,
            shape = CircleShape
        )
        BunnyPart(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = size * 0.56f)
                .size(size * 0.055f, size * 0.04f),
            color = earInner,
            shape = RoundedCornerShape(50)
        )

        // A quiet scarf and the little study journal are the character's signature.
        BunnyPart(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = size * 0.30f)
                .size(size * 0.64f, size * 0.095f)
                .rotate(-4f),
            color = scarf,
            shape = RoundedCornerShape(50)
        )
        BunnyPart(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = size * 0.16f, y = -size * 0.15f)
                .size(size * 0.25f, size * 0.30f)
                .rotate(5f),
            color = book,
            shape = RoundedCornerShape(7.dp)
        )
        BunnyPart(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = size * 0.16f, y = -size * 0.15f)
                .size(size * 0.19f, size * 0.24f)
                .rotate(5f),
            color = Color(0xFFEBE1C9),
            shape = RoundedCornerShape(3.dp)
        )
        BunnyPart(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = size * 0.16f, y = -size * 0.15f)
                .size(size * 0.035f, size * 0.19f),
            color = book,
            shape = RoundedCornerShape(2.dp)
        )

        if (mood == BunnyMood.Celebrating) {
            BunnyPart(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = size * 0.32f, y = size * 0.30f)
                    .size(size * 0.055f),
                color = scarf,
                shape = CircleShape
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
    Box(modifier = modifier.clip(shape).background(color))
}
