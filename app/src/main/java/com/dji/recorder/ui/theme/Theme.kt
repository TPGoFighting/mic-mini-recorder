package com.dji.recorder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.dji.recorder.model.AppThemeMode

// Brand colors
val DjiTeal = Color(0xFF008779)
val DjiTealContainer = Color(0xFFE0F2F1)
val DjiTealDarkContainer = Color(0xFF133834)
val DjiTealLight = Color(0xFF4DB6AC)

val DjiYellow = Color(0xFFFFB800)
val DjiYellowContainer = Color(0xFFFFF3E0)
val DjiYellowDarkContainer = Color(0xFF3E2E10)

val DjiGreen = Color(0xFF00C853)
val DjiRed = Color(0xFFFF5252)

// Dark Palette
val DarkBackground = Color(0xFF111315)
val DarkSurface = Color(0xFF1A1D20)
val DarkSurfaceVariant = Color(0xFF24282D)
val DarkCardBorder = Color(0xFF323842)
val DarkTextPrimary = Color(0xFFF0F2F5)
val DarkTextSecondary = Color(0xFF9EACB9)

// Light Palette
val LightBackground = Color(0xFFF2F5F8)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE8EEF3)
val LightCardBorder = Color(0xFFD6DFE8)
val LightTextPrimary = Color(0xFF191C1E)
val LightTextSecondary = Color(0xFF6B7280)

private val DarkColorScheme = darkColorScheme(
    primary = DjiTealLight,
    onPrimary = Color.Black,
    primaryContainer = DjiTealDarkContainer,
    onPrimaryContainer = DjiTealLight,
    secondary = DjiYellow,
    onSecondary = Color.Black,
    secondaryContainer = DjiYellowDarkContainer,
    onSecondaryContainer = DjiYellow,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkCardBorder,
    error = DjiRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = DjiTeal,
    onPrimary = Color.White,
    primaryContainer = DjiTealContainer,
    onPrimaryContainer = DjiTeal,
    secondary = DjiYellow,
    onSecondary = Color.Black,
    secondaryContainer = DjiYellowContainer,
    onSecondaryContainer = Color(0xFF8A5A00),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightCardBorder,
    error = DjiRed,
    onError = Color.White
)

@Composable
fun DjiRecorderTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
