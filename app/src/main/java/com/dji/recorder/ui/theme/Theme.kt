package com.dji.recorder.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
val NeoBorderLight = Color(0xFF000000)
val NeoShadowLight = Color(0xFF000000)

// Dark 模式背景与底色（深邃极客哑光黑）
val NeoBgDark = Color(0xFF101216)
val NeoSurfaceDark = Color(0xFF1B1E24)
val NeoBorderDark = Color(0xFFFFFFFF)
val NeoShadowDark = Color(0xFFCCFF00) // 暗黑模式下投射酸性绿或高对比硬阴影

// 兼容老引用
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
// 🧱 Neo-Brutalism 原生 Compose 风格化组件
// ==========================================

/**
 * 新粗野风格核心硬阴影卡片 (带 2.5dp 黑色硬边框与实体无模糊阴影)
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    shadowColor: Color = if (isSystemInDarkTheme()) Color(0xFF000000) else Color(0xFF000000),
    borderWidth: Dp = 2.5.dp,
    shadowOffset: Dp = 4.dp,
    shape: Shape = RoundedCornerShape(14.dp),
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
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
        // 表层卡片
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(backgroundColor)
                .border(borderWidth, borderColor, shape)
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(padding),
            content = content
        )
    }
}

/**
 * 新粗野风格实体按压按键
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
        // 按钮本体
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(backgroundColor)
                .border(2.5.dp, borderColor, shape)
                .clickable { onClick() }
                .padding(vertical = 14.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * 新粗野风格工业标签 Badge (黑边框 + 实体底色)
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
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        androidx.compose.material3.Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                letterSpacing = 0.5.sp
            ),
            color = textColor
        )
    }
}
