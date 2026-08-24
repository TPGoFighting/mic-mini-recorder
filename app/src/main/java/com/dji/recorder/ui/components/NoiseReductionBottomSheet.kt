package com.dji.recorder.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.dji.recorder.model.NoiseReductionMode
import com.dji.recorder.ui.theme.DjiTeal
import com.dji.recorder.ui.theme.DjiYellow

/**
 * 降噪质量选择模态弹窗（严格对标设计稿）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoiseReductionBottomSheet(
    currentMode: NoiseReductionMode,
    onSelectMode: (NoiseReductionMode) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 头部标题区域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column {
                    Text(
                        text = "选择降噪质量",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "选择这段录音的处理方式",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 选项 1: 高质量 (AI / DPDFNet2 HR)
            NoiseOptionCard(
                title = NoiseReductionMode.AI_HIGH.title,
                badge = NoiseReductionMode.AI_HIGH.badge,
                description = NoiseReductionMode.AI_HIGH.description,
                icon = Icons.Default.AutoAwesome,
                isSelected = currentMode == NoiseReductionMode.AI_HIGH,
                badgeColor = Color(0xFFFFA000),
                onClick = {
                    onSelectMode(NoiseReductionMode.AI_HIGH)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 选项 2: 低质量 (快速 / Android 硬件降噪)
            NoiseOptionCard(
                title = NoiseReductionMode.FAST_LOW.title,
                badge = NoiseReductionMode.FAST_LOW.badge,
                description = NoiseReductionMode.FAST_LOW.description,
                icon = Icons.Default.GraphicEq,
                isSelected = currentMode == NoiseReductionMode.FAST_LOW,
                badgeColor = Color(0xFFF57C00),
                onClick = {
                    onSelectMode(NoiseReductionMode.FAST_LOW)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 关闭降噪文字按钮
            TextButton(
                onClick = {
                    onSelectMode(NoiseReductionMode.OFF)
                    onDismiss()
                }
            ) {
                Text(
                    text = "关闭降噪",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (currentMode == NoiseReductionMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NoiseOptionCard(
    title: String,
    badge: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    badgeColor: Color,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) borderColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
