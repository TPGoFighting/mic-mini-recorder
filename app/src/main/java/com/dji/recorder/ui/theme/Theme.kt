package com.dji.recorder.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dji.recorder.model.AppThemeMode
import com.dji.recorder.model.AppThemeStyle

// ==========================================
// 🎨 5 大主题色盘 (高对比度设计)
// ==========================================

val NeoBlack = Color(0xFF000000)
val NeoWhite = Color(0xFFFFFFFF)

// 1. 新粗野主义 (Neo-Brutalism)
val NeoAcidLime = Color(0xFFCCFF00)
val NeoCyberYellow = Color(0xFFFFE600)
val NeoHotRed = Color(0xFFFF3366)
val NeoElectricCyan = Color(0xFF00F0FF)
val NeoLavender = Color(0xFFD8B4FE)
val NeoBgLight = Color(0xFFFBF8F2)
val NeoSurfaceLight = Color(0xFFFFFFFF)
val NeoBgDark = Color(0xFF0E1013)
val NeoSurfaceDark = Color(0xFF181B20)

// 2. 极简扁平化 (Flat Design)
val FlatIndigo = Color(0xFF4F46E5)
val FlatIndigoLight = Color(0xFF6366F1)
val FlatEmerald = Color(0xFF10B981)
val FlatAmber = Color(0xFFF59E0B)
val FlatRose = Color(0xFFF43F5E)
val FlatBgLight = Color(0xFFF8FAFC)
val FlatSurfaceLight = Color(0xFFFFFFFF)
val FlatBorderLight = Color(0xFFE2E8F0)
val FlatBgDark = Color(0xFF0B0F19)
val FlatSurfaceDark = Color(0xFF151D2E)
val FlatBorderDark = Color(0xFF222F46)

// 3. 复古拟物化 (Skeuomorphism)
val SkeuoGold = Color(0xFFD4AF37)
val SkeuoGoldDark = Color(0xFF996515)
val SkeuoAmber = Color(0xFFFFB300)
val SkeuoLedRed = Color(0xFFFF3B30)
val SkeuoMetalLightTop = Color(0xFFFFFFFF)
val SkeuoMetalLightBottom = Color(0xFFD4DCE4)
val SkeuoMetalDarkTop = Color(0xFF2E333E)
val SkeuoMetalDarkBottom = Color(0xFF181A20)
val SkeuoBgLight = Color(0xFFDDE4EB)
val SkeuoBgDark = Color(0xFF111317)
val SkeuoBorderLight = Color(0xFF8C9BAE)
val SkeuoBorderDark = Color(0xFF4B5565)

// 4. 软柔新拟态 (Neumorphism)
val NeuBaseLight = Color(0xFFE0E5EC)
val NeuHighlightLight = Color(0xFFFFFFFF)
val NeuShadowLight = Color(0xFFA3B1C2)
val NeuBaseDark = Color(0xFF1E2229)
val NeuHighlightDark = Color(0xFF2B313A)
val NeuShadowDark = Color(0xFF111317)
val NeuCyan = Color(0xFF00BCD4)
val NeuCoral = Color(0xFFFF5722)

// 5. 经典演播室 (Classic Studio)
val ClassicTeal = Color(0xFF008779)
val ClassicTealLight = Color(0xFF4DB6AC)
val ClassicYellow = Color(0xFFFFB800)
val ClassicBgDark = Color(0xFF101215)
val ClassicSurfaceDark = Color(0xFF1A1D22)
val ClassicBgLight = Color(0xFFF3F4F6)
val ClassicSurfaceLight = Color(0xFFFFFFFF)

// 兼容老引用
val DjiGreen = NeoAcidLime
val DjiYellow = NeoCyberYellow
val DjiRed = NeoHotRed
val DjiTeal = NeoElectricCyan

val LocalThemeStyle = staticCompositionLocalOf { AppThemeStyle.NEO_BRUTALISM }

// ==========================================
// 🎨 Color Scheme 工厂 (保证高对比度)
// ==========================================

private fun buildColorScheme(style: AppThemeStyle, isDark: Boolean) = when (style) {
    AppThemeStyle.NEO_BRUTALISM -> if (isDark) {
        darkColorScheme(
            primary = NeoAcidLime, onPrimary = NeoBlack,
            secondary = NeoCyberYellow, onSecondary = NeoBlack,
            background = NeoBgDark, onBackground = NeoWhite,
            surface = NeoSurfaceDark, onSurface = NeoWhite,
            surfaceVariant = Color(0xFF252A32), onSurfaceVariant = Color(0xFFD1D5DB),
            outline = NeoWhite, error = NeoHotRed, onError = NeoWhite
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF008779), onPrimary = NeoWhite,
            secondary = NeoCyberYellow, onSecondary = NeoBlack,
            background = NeoBgLight, onBackground = NeoBlack,
            surface = NeoSurfaceLight, onSurface = NeoBlack,
            surfaceVariant = Color(0xFFEBE6DC), onSurfaceVariant = Color(0xFF1F242D),
            outline = NeoBlack, error = NeoHotRed, onError = NeoWhite
        )
    }

    AppThemeStyle.FLAT_DESIGN -> if (isDark) {
        darkColorScheme(
            primary = FlatIndigoLight, onPrimary = NeoWhite,
            secondary = FlatEmerald, onSecondary = NeoWhite,
            background = FlatBgDark, onBackground = Color(0xFFF8FAFC),
            surface = FlatSurfaceDark, onSurface = Color(0xFFF8FAFC),
            surfaceVariant = Color(0xFF1E293B), onSurfaceVariant = Color(0xFFCBD5E1),
            outline = FlatBorderDark, error = FlatRose, onError = NeoWhite
        )
    } else {
        lightColorScheme(
            primary = FlatIndigo, onPrimary = NeoWhite,
            secondary = FlatEmerald, onSecondary = NeoWhite,
            background = FlatBgLight, onBackground = Color(0xFF0F172A),
            surface = FlatSurfaceLight, onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFF1F5F9), onSurfaceVariant = Color(0xFF334155),
            outline = FlatBorderLight, error = FlatRose, onError = NeoWhite
        )
    }

    AppThemeStyle.SKEUOMORPHISM -> if (isDark) {
        darkColorScheme(
            primary = SkeuoGold, onPrimary = NeoBlack,
            secondary = SkeuoAmber, onSecondary = NeoBlack,
            background = SkeuoBgDark, onBackground = Color(0xFFF3F4F6),
            surface = Color(0xFF22262E), onSurface = Color(0xFFF3F4F6),
            surfaceVariant = Color(0xFF2D323D), onSurfaceVariant = Color(0xFFD1D5DB),
            outline = SkeuoBorderDark, error = SkeuoLedRed, onError = NeoWhite
        )
    } else {
        lightColorScheme(
            primary = SkeuoGoldDark, onPrimary = NeoWhite,
            secondary = SkeuoAmber, onSecondary = NeoBlack,
            background = SkeuoBgLight, onBackground = Color(0xFF111827),
            surface = Color(0xFFFFFFFF), onSurface = Color(0xFF111827),
            surfaceVariant = Color(0xFFE5ECF2), onSurfaceVariant = Color(0xFF374151),
            outline = SkeuoBorderLight, error = SkeuoLedRed, onError = NeoWhite
        )
    }

    AppThemeStyle.NEUMORPHISM -> if (isDark) {
        darkColorScheme(
            primary = NeuCyan, onPrimary = NeoBlack,
            secondary = Color(0xFF81D4FA), onSecondary = NeoBlack,
            background = NeuBaseDark, onBackground = Color(0xFFF1F5F9),
            surface = NeuBaseDark, onSurface = Color(0xFFF1F5F9),
            surfaceVariant = Color(0xFF252A33), onSurfaceVariant = Color(0xFFCBD5E1),
            outline = NeuHighlightDark, error = NeuCoral, onError = NeoWhite
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF00838F), onPrimary = NeoWhite,
            secondary = Color(0xFF00ACC1), onSecondary = NeoWhite,
            background = NeuBaseLight, onBackground = Color(0xFF1E293B),
            surface = NeuBaseLight, onSurface = Color(0xFF1E293B),
            surfaceVariant = Color(0xFFD6DCE5), onSurfaceVariant = Color(0xFF334155),
            outline = NeuShadowLight, error = NeuCoral, onError = NeoWhite
        )
    }

    AppThemeStyle.CLASSIC_STUDIO -> if (isDark) {
        darkColorScheme(
            primary = ClassicTealLight, onPrimary = NeoBlack,
            secondary = ClassicYellow, onSecondary = NeoBlack,
            background = ClassicBgDark, onBackground = Color(0xFFF9FAFB),
            surface = ClassicSurfaceDark, onSurface = Color(0xFFF9FAFB),
            surfaceVariant = Color(0xFF24282E), onSurfaceVariant = Color(0xFFD1D5DB),
            outline = Color(0xFF374151), error = Color(0xFFFF5252), onError = NeoWhite
        )
    } else {
        lightColorScheme(
            primary = ClassicTeal, onPrimary = NeoWhite,
            secondary = ClassicYellow, onSecondary = NeoBlack,
            background = ClassicBgLight, onBackground = Color(0xFF111827),
            surface = ClassicSurfaceLight, onSurface = Color(0xFF111827),
            surfaceVariant = Color(0xFFE5E7EB), onSurfaceVariant = Color(0xFF374151),
            outline = Color(0xFFD1D5DB), error = Color(0xFFFF5252), onError = NeoWhite
        )
    }
}

@Composable
fun DjiRecorderTheme(
    themeStyle: AppThemeStyle = AppThemeStyle.NEO_BRUTALISM,
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val colorScheme = buildColorScheme(themeStyle, isDark)

    CompositionLocalProvider(LocalThemeStyle provides themeStyle) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

// ==========================================
// 🧱 5 大主题自适应卡片组件 (NeoCard)
// ==========================================

@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    shadowColor: Color = Color(0xFF000000),
    borderWidth: Dp = 2.5.dp,
    shadowOffset: Dp = 4.dp,
    shape: Shape = RoundedCornerShape(14.dp),
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val style = LocalThemeStyle.current
    val isDark = isSystemInDarkTheme()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    when (style) {
        AppThemeStyle.NEO_BRUTALISM -> {
            val animTranslate by animateDpAsState(
                targetValue = if (isPressed && onClick != null) 2.5.dp else 0.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "cardNeo"
            )
            Box(modifier = modifier) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = shadowOffset, y = shadowOffset)
                        .clip(shape)
                        .background(shadowColor)
                        .border(borderWidth, shadowColor, shape)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = animTranslate, y = animTranslate)
                        .clip(shape)
                        .background(backgroundColor)
                        .border(borderWidth, borderColor, shape)
                        .then(if (onClick != null) Modifier.clickable(interactionSource, null) { onClick() } else Modifier)
                        .padding(padding),
                    content = content
                )
            }
        }

        AppThemeStyle.FLAT_DESIGN -> {
            val flatShape = RoundedCornerShape(10.dp)
            val animAlpha by animateFloatAsState(targetValue = if (isPressed && onClick != null) 0.85f else 1.0f, label = "cardFlat")
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .scale(animAlpha)
                    .clip(flatShape)
                    .background(backgroundColor)
                    .border(1.dp, borderColor.copy(alpha = 0.6f), flatShape)
                    .then(if (onClick != null) Modifier.clickable(interactionSource, null) { onClick() } else Modifier)
                    .padding(padding),
                content = content
            )
        }

        AppThemeStyle.SKEUOMORPHISM -> {
            val skeuoShape = RoundedCornerShape(14.dp)
            val animScale by animateFloatAsState(targetValue = if (isPressed && onClick != null) 0.98f else 1.0f, label = "cardSkeuo")
            val brush = Brush.verticalGradient(
                colors = if (isDark) listOf(SkeuoMetalDarkTop, SkeuoMetalDarkBottom)
                else listOf(SkeuoMetalLightTop, SkeuoMetalLightBottom)
            )
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .scale(animScale)
                    .shadow(5.dp, skeuoShape)
                    .clip(skeuoShape)
                    .background(brush)
                    .border(1.5.dp, if (isDark) SkeuoBorderDark else SkeuoBorderLight, skeuoShape)
                    .then(if (onClick != null) Modifier.clickable(interactionSource, null) { onClick() } else Modifier)
                    .padding(padding),
                content = content
            )
        }

        AppThemeStyle.NEUMORPHISM -> {
            val neuShape = RoundedCornerShape(18.dp)
            val hiColor = if (isDark) NeuHighlightDark else NeuHighlightLight
            val shColor = if (isDark) NeuShadowDark else NeuShadowLight
            val offsetDist = if (isPressed && onClick != null) 1.dp else 4.dp

            Box(modifier = modifier) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = -offsetDist, y = -offsetDist)
                        .clip(neuShape)
                        .background(hiColor.copy(alpha = 0.85f))
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = offsetDist, y = offsetDist)
                        .clip(neuShape)
                        .background(shColor.copy(alpha = 0.9f))
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(neuShape)
                        .background(backgroundColor)
                        .border(1.dp, hiColor.copy(alpha = 0.5f), neuShape)
                        .then(if (onClick != null) Modifier.clickable(interactionSource, null) { onClick() } else Modifier)
                        .padding(padding),
                    content = content
                )
            }
        }

        AppThemeStyle.CLASSIC_STUDIO -> {
            val classicShape = RoundedCornerShape(16.dp)
            val animScale by animateFloatAsState(targetValue = if (isPressed && onClick != null) 0.98f else 1.0f, label = "cardClassic")
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .scale(animScale)
                    .shadow(3.dp, classicShape)
                    .clip(classicShape)
                    .background(backgroundColor)
                    .border(1.dp, borderColor.copy(alpha = 0.6f), classicShape)
                    .then(if (onClick != null) Modifier.clickable(interactionSource, null) { onClick() } else Modifier)
                    .padding(padding),
                content = content
            )
        }
    }
}

// ==========================================
// 🧱 5 大主题自适应按键组件 (NeoButton)
// ==========================================

@Composable
fun NeoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = NeoAcidLime,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    shadowColor: Color = Color(0xFF000000),
    shadowOffset: Dp = 4.dp,
    shape: Shape = RoundedCornerShape(14.dp),
    content: @Composable RowScope.() -> Unit
) {
    val style = LocalThemeStyle.current
    val isDark = isSystemInDarkTheme()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    when (style) {
        AppThemeStyle.NEO_BRUTALISM -> {
            val animTranslate by animateDpAsState(
                targetValue = if (isPressed) 3.dp else 0.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "btnNeo"
            )
            Box(modifier = modifier) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = shadowOffset, y = shadowOffset)
                        .clip(shape)
                        .background(shadowColor)
                        .border(2.5.dp, shadowColor, shape)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = animTranslate, y = animTranslate)
                        .clip(shape)
                        .background(backgroundColor)
                        .border(2.5.dp, borderColor, shape)
                        .clickable(interactionSource, null) { onClick() }
                        .padding(vertical = 14.dp, horizontal = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }

        AppThemeStyle.FLAT_DESIGN -> {
            val flatShape = RoundedCornerShape(10.dp)
            val animAlpha by animateFloatAsState(targetValue = if (isPressed) 0.85f else 1.0f, label = "btnFlat")
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .scale(animAlpha)
                    .clip(flatShape)
                    .background(backgroundColor)
                    .clickable(interactionSource, null) { onClick() }
                    .padding(vertical = 14.dp, horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        AppThemeStyle.SKEUOMORPHISM -> {
            val skeuoShape = RoundedCornerShape(14.dp)
            val animScale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1.0f, label = "btnSkeuo")
            val brush = Brush.verticalGradient(
                colors = listOf(
                    backgroundColor,
                    backgroundColor.copy(alpha = 0.7f)
                )
            )
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .scale(animScale)
                    .shadow(4.dp, skeuoShape)
                    .clip(skeuoShape)
                    .background(brush)
                    .border(2.dp, if (isDark) Color(0xFF6B7280) else Color(0xFFCBD5E1), skeuoShape)
                    .clickable(interactionSource, null) { onClick() }
                    .padding(vertical = 14.dp, horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        AppThemeStyle.NEUMORPHISM -> {
            val neuShape = RoundedCornerShape(16.dp)
            val hiColor = if (isDark) NeuHighlightDark else NeuHighlightLight
            val shColor = if (isDark) NeuShadowDark else NeuShadowLight
            val offsetDist = if (isPressed) 1.dp else 3.5.dp

            Box(modifier = modifier) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = -offsetDist, y = -offsetDist)
                        .clip(neuShape)
                        .background(hiColor.copy(alpha = 0.85f))
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = offsetDist, y = offsetDist)
                        .clip(neuShape)
                        .background(shColor.copy(alpha = 0.9f))
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(neuShape)
                        .background(backgroundColor)
                        .clickable(interactionSource, null) { onClick() }
                        .padding(vertical = 14.dp, horizontal = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }

        AppThemeStyle.CLASSIC_STUDIO -> {
            val classicShape = RoundedCornerShape(14.dp)
            val animScale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1.0f, label = "btnClassic")
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .scale(animScale)
                    .shadow(3.dp, classicShape)
                    .clip(classicShape)
                    .background(backgroundColor)
                    .clickable(interactionSource, null) { onClick() }
                    .padding(vertical = 14.dp, horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

// ==========================================
// 🧱 5 大主题自适应标签组件 (NeoBadge) - 彻底杜绝文字折行
// ==========================================

@Composable
fun NeoBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = NeoCyberYellow,
    textColor: Color = NeoBlack,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = 1.5.dp,
    shape: Shape = RoundedCornerShape(6.dp),
    onClick: (() -> Unit)? = null
) {
    val style = LocalThemeStyle.current
    val isNeo = style == AppThemeStyle.NEO_BRUTALISM

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animScale by animateDpAsState(
        targetValue = if (isPressed && onClick != null && isNeo) 1.5.dp else 0.dp,
        label = "badgeScale"
    )

    Box(
        modifier = modifier
            .offset(x = animScale, y = animScale)
            .clip(if (isNeo) shape else RoundedCornerShape(6.dp))
            .background(if (isNeo) backgroundColor else backgroundColor.copy(alpha = 0.22f))
            .then(
                if (isNeo) Modifier.border(borderWidth, borderColor, shape)
                else Modifier
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource, null) { onClick() }
                } else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                fontSize = 11.sp
            ),
            color = if (isNeo) textColor else MaterialTheme.colorScheme.primary
        )
    }
}
