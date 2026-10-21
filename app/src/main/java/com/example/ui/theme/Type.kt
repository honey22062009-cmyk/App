package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AppFont = FontFamily.SansSerif

private fun appTextStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal
) = TextStyle(
    fontFamily = AppFont,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp
)

val Typography = Typography(
    displayLarge = appTextStyle(30, 38, FontWeight.SemiBold),
    displayMedium = appTextStyle(26, 34, FontWeight.SemiBold),
    displaySmall = appTextStyle(24, 32, FontWeight.SemiBold),
    headlineLarge = appTextStyle(24, 32, FontWeight.SemiBold),
    headlineMedium = appTextStyle(20, 28, FontWeight.SemiBold),
    headlineSmall = appTextStyle(18, 26, FontWeight.Medium),
    titleLarge = appTextStyle(17, 24, FontWeight.SemiBold),
    titleMedium = appTextStyle(15, 22, FontWeight.Medium),
    titleSmall = appTextStyle(14, 20, FontWeight.Medium),
    bodyLarge = appTextStyle(16, 24),
    bodyMedium = appTextStyle(14, 21),
    bodySmall = appTextStyle(12, 18),
    labelLarge = appTextStyle(14, 20, FontWeight.Medium),
    labelMedium = appTextStyle(12, 17, FontWeight.Medium),
    labelSmall = appTextStyle(11, 16, FontWeight.Medium)
)
