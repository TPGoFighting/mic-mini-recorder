package com.dji.recorder.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dji.recorder.model.AppThemeMode

// ==========================================
// 🎨 新粗野主义 (Neo-Brutalism) 核心高能色盘
// ==========================================

val NeoBlack = Color(0xFF000000)
val NeoWhite = Color(0xFFFFFFFF)

// 高能酸性撞色
val NeoAcidLime = Color(0xFFCCFF00)    // 核心酸性电光绿 (品牌主色 / 安全 / 就绪)
val NeoCyberYellow = Color(0xFFFFE600) // 高能波普黄 (降噪 / 警告 / 格式)
val NeoHotRed = Color(0xFFFF3366)      // 热辣电光红 (录音 REC / 停止按钮 / 严重告警)
val NeoElectricCyan = Color(0xFF00F0FF)// 极客电光青 (USB 状态 / 传输 / 刷新)
val NeoLavender = Color(0xFFD8B4FE)    // 浅紫粉晶
val NeoOrange = Color(0xFFFF7A00)      // 活力亮橙

// Light 模式背景与底色（复古暖米白纸张感）
val NeoBgLight = Color(0xFFF7F4EC)
val NeoSurfaceLight = Color(0xFFFFFFFF)

// Dark 模式背景与底色（深邃极客哑光黑）
val NeoBgDark = Color(0xFF101216)
val NeoSurfaceDark = Color(0xFF1B1E24)

// 兼容引用
val DjiGreen = NeoAcidLime
val DjiYellow = NeoCyberYellow
val DjiRed = NeoHotRed
val DjiTeal = NeoElectricCyan

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

    val colorScheme = if (isDark) NeoDarkColorScheme else NeoLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// ==========================================
// 🧱 Neo-Brutalism 交互式动效组件
// ==========================================

/**
 * 带有【物理按压回弹机械动效】的新粗野核心卡片
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 机械下沉按压动效
    val animTranslate by animateDpAsState(
        targetValue = if (isPressed && onClick != null) 2.5.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "cardTranslate"
    )

    Box(modifier = modifier) {
        // 底层实体硬阴影
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .clip(shape)
                .background(shadowColor)
                .border(borderWidth, shadowColor, shape)
        )
        // 表层卡片（按压时向右下物理下陷）
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
}

/**
 * 带有【街机微动开关按压下陷动效】的新粗野按键
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animTranslate by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "btnTranslate"
    )

    Box(modifier = modifier) {
        // 阴影层
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .clip(shape)
                .background(shadowColor)
                .border(2.5.dp, shadowColor, shape)
        )
        // 按钮本体 (按压时下陷)
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
}

/**
 * 新粗野风格互动标签 Badge (支持轻触触感)
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animScale by animateDpAsState(
        targetValue = if (isPressed && onClick != null) 1.5.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "badgeScale"
    )

    Box(
        modifier = modifier
            .offset(x = animScale, y = animScale)
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
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            ),
            color = textColor
        )
    }
}
