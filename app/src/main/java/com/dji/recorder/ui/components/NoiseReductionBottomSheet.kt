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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.VolumeOff
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
import com.dji.recorder.model.NoiseReductionMode
import com.dji.recorder.ui.theme.LocalThemeStyle
import com.dji.recorder.ui.theme.NeoAcidLime
import com.dji.recorder.ui.theme.NeoBadge
import com.dji.recorder.ui.theme.NeoBlack
import com.dji.recorder.ui.theme.NeoCyberYellow
import com.dji.recorder.ui.theme.NeoLavender

/**
 * 降噪质量选择模态面板 (高对比度、防折行排版)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoiseReductionBottomSheet(
    currentMode: NoiseReductionMode,
    onSelectMode: (NoiseReductionMode) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val borderColor = MaterialTheme.colorScheme.outline
    val style = LocalThemeStyle.current

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
                .padding(bottom = 36.dp, top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 头部标题区域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (style == AppThemeStyle.NEO_BRUTALISM) NeoLavender else MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        if (style == AppThemeStyle.NEO_BRUTALISM) 2.5.dp else 1.dp,
                        borderColor,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (style == AppThemeStyle.NEO_BRUTALISM) NeoCyberYellow else MaterialTheme.colorScheme.primary)
                        .border(1.5.dp, borderColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = null,
                        tint = if (style == AppThemeStyle.NEO_BRUTALISM) NeoBlack else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "NOISE REDUCTION ENGINE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = if (style == AppThemeStyle.NEO_BRUTALISM) NeoBlack else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "选择音频降噪滤波算法处理方式",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (style == AppThemeStyle.NEO_BRUTALISM) NeoBlack.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 选项 1: 原声直通 (推荐)
            NeoNoiseOptionCard(
                title = NoiseReductionMode.OFF.title,
                badge = NoiseReductionMode.OFF.badge,
                description = NoiseReductionMode.OFF.description,
                icon = Icons.Default.VolumeOff,
                isSelected = currentMode == NoiseReductionMode.OFF,
                accentColor = NeoAcidLime,
                onClick = {
                    onSelectMode(NoiseReductionMode.OFF)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 选项 2: 演播室专业降噪 (Audacity / WebRTC APM)
            NeoNoiseOptionCard(
                title = NoiseReductionMode.AI_HIGH.title,
                badge = NoiseReductionMode.AI_HIGH.badge,
                description = NoiseReductionMode.AI_HIGH.description,
                icon = Icons.Default.AutoAwesome,
                isSelected = currentMode == NoiseReductionMode.AI_HIGH,
                accentColor = NeoCyberYellow,
                onClick = {
                    onSelectMode(NoiseReductionMode.AI_HIGH)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 选项 3: 系统快速降噪
            NeoNoiseOptionCard(
                title = NoiseReductionMode.FAST_LOW.title,
                badge = NoiseReductionMode.FAST_LOW.badge,
                description = NoiseReductionMode.FAST_LOW.description,
                icon = Icons.Default.GraphicEq,
                isSelected = currentMode == NoiseReductionMode.FAST_LOW,
                accentColor = NeoLavender,
                onClick = {
                    onSelectMode(NoiseReductionMode.FAST_LOW)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun NeoNoiseOptionCard(
    title: String,
    badge: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val style = LocalThemeStyle.current
    val borderColor = MaterialTheme.colorScheme.outline
    val isNeo = style == AppThemeStyle.NEO_BRUTALISM

    Box(modifier = Modifier.fillMaxWidth()) {
        // 底层实体硬阴影 (Neo-Brutalism)
        if (isNeo && isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 3.dp, y = 3.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            )
        }

        // 卡片表层 (确保 100% 对比度与清晰文字)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) {
                        if (isNeo) accentColor.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    if (isSelected) (if (isNeo) 2.5.dp else 2.dp) else 1.dp,
                    if (isSelected) (if (isNeo) borderColor else MaterialTheme.colorScheme.primary)
                    else borderColor.copy(alpha = 0.35f),
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) accentColor else MaterialTheme.colorScheme.surface)
                            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) NeoBlack else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                NeoBadge(
                    text = badge,
                    backgroundColor = if (isSelected) accentColor else MaterialTheme.colorScheme.surface,
                    textColor = NeoBlack
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp, fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
