package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = TpPurplePrimary,
    onPrimary = Color.White,
    primaryContainer = TpPurpleSoft,
    onPrimaryContainer = TpPurplePrimary,
    secondary = TpPurpleSecondary,
    onSecondary = Color.White,
    secondaryContainer = TpPurpleSoft,
    onSecondaryContainer = TpPurplePrimary,
    tertiary = TpGreen,
    onTertiary = Color.White,
    background = TpBgLight,
    onBackground = TpTextPrimary,
    surface = TpSurfaceLight,
    onSurface = TpTextPrimary,
    surfaceVariant = TpSurfaceElevated,
    onSurfaceVariant = TpTextSecondary,
    outline = TpBorderLight,
    outlineVariant = TpBorderSubtle,
    error = TpCoral,
    onError = Color.White,
    errorContainer = TpCoralSoft,
    onErrorContainer = TpCoralDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
