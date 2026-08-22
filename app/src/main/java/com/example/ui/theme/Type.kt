package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography =
  Typography(
    bodyLarge =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
      ),
    titleLarge = TextStyle(
      fontFamily = FontFamily.Serif,
      fontWeight = FontWeight.Bold,
      fontSize = 24.sp,
      lineHeight = 29.sp,
      letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Medium,
      fontSize = 40.sp,
      lineHeight = 44.sp,
      letterSpacing = (-1.2).sp,
    ),
    labelSmall = TextStyle(
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Medium,
      fontSize = 11.sp,
      lineHeight = 16.sp,
      letterSpacing = 0.3.sp,
    )
  )
