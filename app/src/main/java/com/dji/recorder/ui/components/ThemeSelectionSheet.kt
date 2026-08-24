package com.dji.recorder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dji.recorder.model.AppThemeStyle
import com.dji.recorder.ui.theme.ClassicTeal
import com.dji.recorder.ui.theme.ClassicYellow
import com.dji.recorder.ui.theme.FlatAmber
import com.dji.recorder.ui.theme.FlatEmerald
import com.dji.recorder.ui.theme.FlatIndigo
import com.dji.recorder.ui.theme.NeoAcidLime
import com.dji.recorder.ui.theme.NeoBadge
import com.dji.recorder.ui.theme.NeoBlack
import com.dji.recorder.ui.theme.NeoCyberYellow
import com.dji.recorder.ui.theme.NeoElectricCyan
import com.dji.recorder.ui.theme.NeoHotRed
import com.dji.recorder.ui.theme.NeuCyan
import com.dji.recorder.ui.theme.SkeuoAmber
import com.dji.recorder.ui.theme.SkeuoGold

/**
 * 独立的【全套视觉主题选择】模态面板 (支持 5 大顶级工业设计主题)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionSheet(
    currentThemeStyle: AppThemeStyle,
    onSelectThemeStyle: (AppThemeStyle) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val borderColor = MaterialTheme.colorScheme.outline

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp, top = 4.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 顶栏 Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NeoCyberYellow)
                    .border(2.5.dp, borderColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeoBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = NeoCyberYellow,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "DESIGN THEME GALLERY",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = NeoBlack
                        )
                        Text(
                            text = "切换 5 大顶级工业 UI 视觉风格",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NeoBlack.copy(alpha = 0.75f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeoBlack)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5 大主题卡片列表
            AppThemeStyle.entries.forEach { style ->
                val isSelected = currentThemeStyle == style
                ThemeOptionCard(
                    style = style,
                    isSelected = isSelected,
                    onClick = {
                        onSelectThemeStyle(style)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ThemeOptionCard(
    style: AppThemeStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outline

    val icon: ImageVector = when (style) {
        AppThemeStyle.NEO_BRUTALISM -> Icons.Default.Bolt
        AppThemeStyle.FLAT_DESIGN -> Icons.Default.Layers
        AppThemeStyle.SKEUOMORPHISM -> Icons.Default.Tune
        AppThemeStyle.NEUMORPHISM -> Icons.Default.BlurOn
        AppThemeStyle.CLASSIC_STUDIO -> Icons.Default.Mic
    }

    val paletteColors = when (style) {
        AppThemeStyle.NEO_BRUTALISM -> listOf(NeoAcidLime, NeoCyberYellow, NeoHotRed, NeoElectricCyan)
        AppThemeStyle.FLAT_DESIGN -> listOf(FlatIndigo, FlatEmerald, FlatAmber, Color(0xFFF43F5E))
        AppThemeStyle.SKEUOMORPHISM -> listOf(SkeuoGold, SkeuoAmber, Color(0xFF374151), Color(0xFFE5E7EB))
        AppThemeStyle.NEUMORPHISM -> listOf(NeuCyan, Color(0xFFE0E5EC), Color(0xFF90A4AE), Color(0xFF263238))
        AppThemeStyle.CLASSIC_STUDIO -> listOf(ClassicTeal, ClassicYellow, Color(0xFF00C853), Color(0xFF1E2228))
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        // 底层实体硬阴影
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 3.dp, y = 3.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            )
        }

        // 卡片本体
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    if (isSelected) 2.5.dp else 1.5.dp,
                    if (isSelected) borderColor else borderColor.copy(alpha = 0.4f),
                    RoundedCornerShape(12.dp)
                )
                .clickable { onClick() }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeoAcidLime else MaterialTheme.colorScheme.surface)
                            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) NeoBlack else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = style.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            NeoBadge(
                                text = style.badge,
                                backgroundColor = if (isSelected) NeoCyberYellow else MaterialTheme.colorScheme.surface,
                                textColor = NeoBlack
                            )
                        }
                    }
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(NeoAcidLime)
                            .border(1.5.dp, borderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = NeoBlack,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = style.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 调色盘预览圆点
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PALETTE:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                paletteColors.forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(1.dp, borderColor.copy(alpha = 0.5f), CircleShape)
                    )
                }
            }
        }
    }
}
