package com.dji.recorder.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dji.recorder.ui.theme.NeoAcidLime
import com.dji.recorder.ui.theme.NeoBlack
import com.dji.recorder.ui.theme.NeoCyberYellow
import com.dji.recorder.ui.theme.NeoHotRed

/**
 * 新粗野风格 (Neo-Brutalism) 实时动态声波可视化组件与工业 VU 电平表
 */
@Composable
fun WaveformVisualizer(
    amplitudes: List<Float>,
    currentDecibels: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.outline

    Box(modifier = modifier.fillMaxWidth()) {
        // 底层实体硬阴影
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
                .border(2.5.dp, Color.Black, RoundedCornerShape(14.dp))
        )

        // 表层卡片
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(2.5.dp, borderColor, RoundedCornerShape(14.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 顶栏指标
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isRecording) NeoHotRed else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.5.dp, borderColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isRecording) "● LIVE VU" else "■ IDLE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (isRecording) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "SPECTRUM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (currentDecibels > -3f) NeoHotRed else NeoAcidLime)
                        .border(1.5.dp, borderColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isRecording) String.format("%.1f dB", currentDecibels) else "-∞ dB",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = if (currentDecibels > -3f) Color.White else NeoBlack
                    )
                }
            }

            // 声波条带渲染 (高饱和方块条带 + 黑边勾勒)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.5.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2f
                    val barCount = 32
                    val barWidth = 6.5.dp.toPx()
                    val gap = (width - (barCount * barWidth)) / (barCount - 1)

                    val displayData = if (amplitudes.size >= barCount) {
                        amplitudes.takeLast(barCount)
                    } else {
                        List(barCount - amplitudes.size) { 0.05f } + amplitudes
                    }

                    for (i in displayData.indices) {
                        val x = i * (barWidth + gap)
                        val amp = if (isRecording) displayData[i] else 0.05f
                        val barHeight = (amp.coerceIn(0.06f, 1.0f) * height).coerceAtLeast(6.dp.toPx())

                        val barColor = when {
                            amp > 0.75f -> NeoHotRed
                            amp > 0.40f -> NeoCyberYellow
                            else -> NeoAcidLime
                        }

                        // 绘制实体方块条
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, centerY - (barHeight / 2f)),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }

            // 底部工业 VU 进度条
            val animLevel = remember { Animatable(0f) }
            val targetRatio = if (isRecording) {
                ((currentDecibels + 60f) / 60f).coerceIn(0f, 1f)
            } else 0f

            LaunchedEffect(targetRatio) {
                animLevel.animateTo(targetRatio, tween(50))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .border(1.5.dp, borderColor, RoundedCornerShape(3.dp))
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val activeWidth = size.width * animLevel.value
                    drawRect(
                        color = when {
                            animLevel.value > 0.85f -> NeoHotRed
                            animLevel.value > 0.65f -> NeoCyberYellow
                            else -> NeoAcidLime
                        },
                        topLeft = Offset.Zero,
                        size = Size(activeWidth, size.height)
                    )
                }
            }
        }
    }
}
