package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TpBorderLight
import com.example.ui.theme.TpBorderSubtle
import com.example.ui.theme.TpCoral
import com.example.ui.theme.TpPurpleLight
import com.example.ui.theme.TpPurplePrimary
import com.example.ui.theme.TpPurpleSecondary
import com.example.ui.theme.TpPurpleSoft
import com.example.ui.theme.TpSurfaceLight
import com.example.ui.theme.TpTextMuted
import com.example.ui.theme.TpTextPrimary
import com.example.ui.theme.TpTextSecondary
import java.util.Locale

@Composable
fun WaveformVisualizer(
    amplitudes: List<Float>,
    rmsDb: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedDb by animateFloatAsState(
        targetValue = if (isRecording) rmsDb else -96f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "dbAnimation"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp), spotColor = Color(0x0C000000))
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, TpBorderLight, RoundedCornerShape(24.dp)),
        color = TpSurfaceLight
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Waveform Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
                    .testTag("waveform_canvas"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(105.dp)) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val barCount = amplitudes.size.coerceAtLeast(1)
                    val totalSpacing = canvasWidth * 0.28f
                    val barWidth = ((canvasWidth - totalSpacing) / barCount).coerceAtLeast(3.5f)
                    val spacing = totalSpacing / (barCount + 1)

                    val gradient = Brush.verticalGradient(
                        colors = if (isRecording) {
                            listOf(TpPurpleLight, TpPurplePrimary)
                        } else {
                            listOf(TpPurpleLight.copy(alpha = 0.45f), TpBorderLight)
                        }
                    )

                    for (i in amplitudes.indices) {
                        val amp = if (isRecording) amplitudes[i].coerceIn(0.08f, 1.0f) else 0.08f
                        val barHeight = (canvasHeight * amp * 0.88f).coerceIn(4f, canvasHeight * 0.92f)
                        val x = spacing + i * (barWidth + spacing)
                        val y = (canvasHeight - barHeight) / 2f

                        val alphaMultiplier = if (isRecording) {
                            val centerDist = kotlin.math.abs(i - barCount / 2f) / (barCount / 2f)
                            (1f - centerDist * 0.35f).coerceIn(0.45f, 1.0f)
                        } else 0.4f

                        drawRoundRect(
                            brush = gradient,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                            alpha = alphaMultiplier
                        )
                    }
                }

                if (!isRecording) {
                    Text(
                        text = "等待录音开始...",
                        color = TpTextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // dB Meter Bar & Label
            val dbNormalized = ((animatedDb + 96f) / 96f).coerceIn(0f, 1f)
            val meterColor = when {
                animatedDb > -6f -> TpCoral
                else -> TpPurplePrimary
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "电平 (RMS)",
                    fontSize = 12.sp,
                    color = TpTextSecondary,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = if (isRecording) String.format(Locale.US, "%.1f dBFS", animatedDb) else "-- dB",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) meterColor else TpTextMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { dbNormalized },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = meterColor,
                trackColor = TpBorderSubtle,
            )
        }
    }
}
