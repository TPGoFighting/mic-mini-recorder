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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dji.recorder.model.AppThemeMode
import com.dji.recorder.model.AppThemeStyle

// ==========================================
// 🎨 主题色彩系统定义
// ==========================================

// 基础黑白
val NeoBlack = Color(0xFF000000)
val NeoWhite = Color(0xFFFFFFFF)

// 1. 新粗野主义 (Neo-Brutalism) 酸性撞色盘
val NeoAcidLime = Color(0xFFCCFF00)    // 酸性电光绿 (品牌主色 / 安全 / 就绪)
val NeoCyberYellow = Color(0xFFFFE600) // 高能波普黄 (降噪 / 警告 / 格式)
val NeoHotRed = Color(0xFFFF3366)      // 热辣电光红 (录音 REC / 停止按钮 / 严重告警)
val NeoElectricCyan = Color(0xFF00F0FF)// 极客电光青 (USB 状态 / 传输 / 刷新)
val NeoLavender = Color(0xFFD8B4FE)    // 浅紫粉晶
val NeoOrange = Color(0xFFFF7A00)      // 活力亮橙

val NeoBgLight = Color(0xFFF7F4EC)
val NeoSurfaceLight = Color(0xFFFFFFFF)
val NeoBgDark = Color(0xFF101216)
val NeoSurfaceDark = Color(0xFF1B1E24)

// 2. 经典演播室 (Classic Studio) 大疆原厂雅致色盘
val ClassicTeal = Color(0xFF008779)
val ClassicTealLight = Color(0xFF4DB6AC)
val ClassicTealContainer = Color(0xFFE0F2F1)
val ClassicTealDarkContainer = Color(0xFF133834)
val ClassicYellow = Color(0xFFFFB800)
val ClassicGreen = Color(0xFF00C853)
val ClassicRed = Color(0xFFFF5252)

val ClassicBgDark = Color(0xFF111315)
val ClassicSurfaceDark = Color(0xFF1A1D20)
val ClassicSurfaceVariantDark = Color(0xFF24282D)
val ClassicBorderDark = Color(0xFF323842)
val ClassicTextPrimaryDark = Color(0xFFF0F2F5)
val ClassicTextSecondaryDark = Color(0xFF9EACB9)

val ClassicBgLight = Color(0xFFF2F5F8)
val ClassicSurfaceLight = Color(0xFFFFFFFF)
val ClassicSurfaceVariantLight = Color(0xFFE8EEF3)
val ClassicBorderLight = Color(0xFFD6DFE8)
val ClassicTextPrimaryLight = Color(0xFF191C1E)
val ClassicTextSecondaryLight = Color(0xFF6B7280)

// 兼容老命名
val DjiGreen = NeoAcidLime
val DjiYellow = NeoCyberYellow
val DjiRed = NeoHotRed
val DjiTeal = NeoElectricCyan

// CompositionLocal 传递当前视觉风格
val LocalThemeStyle = staticCompositionLocalOf { AppThemeStyle.NEO_BRUTALISM }

// ==========================================
// 🎨 Color Schemes
// ==========================================

// Neo-Brutalism Color Schemes
private val NeoDarkColorScheme = darkColorScheme(
    primary = NeoAcidLime,
    onPrimary = NeoBlack,
    primaryContainer = Color(0xFF283618),
    onPrimaryContainer = NeoAcidLime,
    secondary = NeoCyberYellow,
    onSecondary = NeoBlack,
    secondaryContainer = Color(0xFF3E3610),
    onSecondaryContainer = NeoCyberYellow,
    background = NeoBgDark,
    onBackground = NeoWhite,
    surface = NeoSurfaceDark,
    onSurface = NeoWhite,
    surfaceVariant = Color(0xFF262B34),
    onSurfaceVariant = Color(0xFFB0BAC5),
    outline = Color(0xFFFFFFFF),
    error = NeoHotRed,
    onError = NeoWhite
)

private val NeoLightColorScheme = lightColorScheme(
    primary = Color(0xFF008779),
    onPrimary = NeoWhite,
    primaryContainer = NeoAcidLime,
    onPrimaryContainer = NeoBlack,
    secondary = NeoCyberYellow,
    onSecondary = NeoBlack,
    secondaryContainer = Color(0xFFFFF7C2),
    onSecondaryContainer = NeoBlack,
    background = NeoBgLight,
    onBackground = NeoBlack,
    surface = NeoSurfaceLight,
    onSurface = NeoBlack,
    surfaceVariant = Color(0xFFEBE6DC),
    onSurfaceVariant = Color(0xFF333333),
    outline = NeoBlack,
    error = NeoHotRed,
    onError = NeoWhite
)

// Classic Studio Color Schemes
private val ClassicDarkColorScheme = darkColorScheme(
    primary = ClassicTealLight,
    onPrimary = Color.Black,
    primaryContainer = ClassicTealDarkContainer,
    onPrimaryContainer = ClassicTealLight,
    secondary = ClassicYellow,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3E2E10),
    onSecondaryContainer = ClassicYellow,
    background = ClassicBgDark,
    onBackground = ClassicTextPrimaryDark,
    surface = ClassicSurfaceDark,
    onSurface = ClassicTextPrimaryDark,
    surfaceVariant = ClassicSurfaceVariantDark,
    onSurfaceVariant = ClassicTextSecondaryDark,
    outline = ClassicBorderDark,
    error = ClassicRed,
    onError = Color.White
)

private val ClassicLightColorScheme = lightColorScheme(
    primary = ClassicTeal,
    onPrimary = Color.White,
    primaryContainer = ClassicTealContainer,
    onPrimaryContainer = ClassicTeal,
    secondary = ClassicYellow,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFF3E0),
    onSecondaryContainer = Color(0xFF8A5A00),
    background = ClassicBgLight,
    onBackground = ClassicTextPrimaryLight,
    surface = ClassicSurfaceLight,
    onSurface = ClassicTextPrimaryLight,
    surfaceVariant = ClassicSurfaceVariantLight,
    onSurfaceVariant = ClassicTextSecondaryLight,
    outline = ClassicBorderLight,
    error = ClassicRed,
    onError = Color.White
)

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

    val colorScheme = when (themeStyle) {
        AppThemeStyle.NEO_BRUTALISM -> if (isDark) NeoDarkColorScheme else NeoLightColorScheme
        AppThemeStyle.CLASSIC_STUDIO -> if (isDark) ClassicDarkColorScheme else ClassicLightColorScheme
    }

    CompositionLocalProvider(LocalThemeStyle provides themeStyle) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

// ==========================================
// 🧱 自适应视觉风格化组件 (Neo-Brutalism & Classic Studio)
// ==========================================

/**
 * 自适应卡片 (在 Neo-Brutalism 下表现为硬边框+硬阴影+机械下陷；在 Classic Studio 下表现为圆润柔和微阴影)
 */
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
    val currentStyle = LocalThemeStyle.current
    val isNeo = currentStyle == AppThemeStyle.NEO_BRUTALISM

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animTranslate by animateDpAsState(
        targetValue = if (isPressed && onClick != null && isNeo) 2.5.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "cardTranslate"
    )

    val classicScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null && !isNeo) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "classicScale"
    )

    if (isNeo) {
        // Neo-Brutalism: 粗黑硬阴影 + 机械下沉
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
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onClick() }
                        } else Modifier
                    )
                    .padding(padding),
                content = content
            )
        }
    } else {
        // Classic Studio: 柔和圆润卡片 + 轻微浮雕
        Column(
            modifier = modifier
                .fillMaxWidth()
                .scale(classicScale)
                .shadow(2.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(backgroundColor)
                .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onClick() }
                    } else Modifier
                )
                .padding(padding),
            content = content
        )
    }
}

/**
 * 自适应实体按压按键
 */
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
    val currentStyle = LocalThemeStyle.current
    val isNeo = currentStyle == AppThemeStyle.NEO_BRUTALISM

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animTranslate by animateDpAsState(
        targetValue = if (isPressed && isNeo) 3.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "btnTranslate"
    )

    val classicScale by animateFloatAsState(
        targetValue = if (isPressed && !isNeo) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "classicBtnScale"
    )

    if (isNeo) {
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
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onClick() }
                    .padding(vertical = 14.dp, horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .scale(classicScale)
                .shadow(3.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() }
                .padding(vertical = 14.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * 自适应标签 Badge
 */
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
    val currentStyle = LocalThemeStyle.current
    val isNeo = currentStyle == AppThemeStyle.NEO_BRUTALISM

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animScale by animateDpAsState(
        targetValue = if (isPressed && onClick != null && isNeo) 1.5.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "badgeScale"
    )

    Box(
        modifier = modifier
            .offset(x = animScale, y = animScale)
            .clip(if (isNeo) shape else RoundedCornerShape(8.dp))
            .background(if (isNeo) backgroundColor else backgroundColor.copy(alpha = 0.18f))
            .then(
                if (isNeo) Modifier.border(borderWidth, borderColor, shape)
                else Modifier
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onClick() }
                } else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            ),
            color = if (isNeo) textColor else MaterialTheme.colorScheme.primary
        )
    }
}
