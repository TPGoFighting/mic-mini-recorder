package com.dji.recorder.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dji.recorder.ui.theme.DjiGreen
import com.dji.recorder.ui.theme.DjiRed
import com.dji.recorder.ui.theme.DjiYellow

/**
 * 实时动态声波可视化组件与 VU 电平表
 */
@Composable
fun WaveformVisualizer(
    amplitudes: List<Float>,
    currentDecibels: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 声波条带渲染
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val height = size.height
                val centerY = height / 2f
                val barCount = 36
                val barWidth = 6.dp.toPx()
                val gap = (width - (barCount * barWidth)) / (barCount - 1)

                val displayData = if (amplitudes.size >= barCount) {
                    amplitudes.takeLast(barCount)
                } else {
                    List(barCount - amplitudes.size) { 0.05f } + amplitudes
                }

                for (i in displayData.indices) {
                    val x = i * (barWidth + gap)
                    val amp = if (isRecording) displayData[i] else 0.05f
                    val barHeight = (amp.coerceIn(0.04f, 1.0f) * height).coerceAtLeast(4.dp.toPx())

                    val brush = Brush.verticalGradient(
                        colors = listOf(
                            DjiYellow,
                            DjiYellow.copy(alpha = 0.6f)
                        )
                    )

                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(x, centerY - (barHeight / 2f)),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
        }

        // 底部 VU 电平表与 dB 指示
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "INPUT LEVEL",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (isRecording) String.format("%.1f dB", currentDecibels) else "-∞ dB",
                style = MaterialTheme.typography.labelMedium,
                color = if (currentDecibels > -3f) DjiRed else DjiGreen
            )
        }

        // VU 电平条
        val animLevel = remember { Animatable(0f) }
        val targetRatio = if (isRecording) {
            ((currentDecibels + 60f) / 60f).coerceIn(0f, 1f)
        } else 0f

        LaunchedEffect(targetRatio) {
            animLevel.animateTo(targetRatio, tween(60))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val activeWidth = size.width * animLevel.value
                val gradient = Brush.horizontalGradient(
                    0.0f to DjiGreen,
                    0.75f to DjiYellow,
                    1.0f to DjiRed
                )
                drawRoundRect(
                    brush = gradient,
                    topLeft = Offset.Zero,
                    size = Size(activeWidth, size.height),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
        }
    }
}
