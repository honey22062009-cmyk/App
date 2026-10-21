package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AppFont = FontFamily.SansSerif

val Typography = Typography(
    displayLarge = TextStyle(AppFont, FontWeight.SemiBold, 30.sp, 38.sp),
    displayMedium = TextStyle(AppFont, FontWeight.SemiBold, 26.sp, 34.sp),
    displaySmall = TextStyle(AppFont, FontWeight.SemiBold, 24.sp, 32.sp),
    headlineLarge = TextStyle(AppFont, FontWeight.SemiBold, 24.sp, 32.sp),
    headlineMedium = TextStyle(AppFont, FontWeight.SemiBold, 20.sp, 28.sp),
    headlineSmall = TextStyle(AppFont, FontWeight.Medium, 18.sp, 26.sp),
    titleLarge = TextStyle(AppFont, FontWeight.SemiBold, 17.sp, 24.sp),
    titleMedium = TextStyle(AppFont, FontWeight.Medium, 15.sp, 22.sp),
    titleSmall = TextStyle(AppFont, FontWeight.Medium, 14.sp, 20.sp),
    bodyLarge = TextStyle(AppFont, FontWeight.Normal, 16.sp, 24.sp),
    bodyMedium = TextStyle(AppFont, FontWeight.Normal, 14.sp, 21.sp),
    bodySmall = TextStyle(AppFont, FontWeight.Normal, 12.sp, 18.sp),
    labelLarge = TextStyle(AppFont, FontWeight.Medium, 14.sp, 20.sp),
    labelMedium = TextStyle(AppFont, FontWeight.Medium, 12.sp, 17.sp),
    labelSmall = TextStyle(AppFont, FontWeight.Medium, 11.sp, 16.sp)
)
