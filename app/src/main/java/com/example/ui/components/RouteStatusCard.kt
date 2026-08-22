package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioRouteInfo
import com.example.ui.theme.TpBorderLight
import com.example.ui.theme.TpCoral
import com.example.ui.theme.TpCoralSoft
import com.example.ui.theme.TpGreen
import com.example.ui.theme.TpPurplePrimary
import com.example.ui.theme.TpPurpleSecondary
import com.example.ui.theme.TpPurpleSoft
import com.example.ui.theme.TpSurfaceElevated
import com.example.ui.theme.TpSurfaceLight
import com.example.ui.theme.TpTextMuted
import com.example.ui.theme.TpTextPrimary
import com.example.ui.theme.TpTextSecondary

@Composable
fun RouteStatusCard(
    routeInfo: AudioRouteInfo,
    strictBluetoothOnly: Boolean,
    onRefreshRoute: () -> Unit,
    onSelectMicClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isExternalBt = routeInfo.isExternalBluetooth

    val statusDotColor = when {
        isExternalBt -> TpGreen
        !strictBluetoothOnly -> TpPurplePrimary
        else -> TpCoral
    }

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp), spotColor = Color(0x0C000000))
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, TpBorderLight, RoundedCornerShape(24.dp))
            .testTag("route_status_card"),
        color = TpSurfaceLight
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(statusDotColor.copy(alpha = if (isExternalBt) alpha else 1f))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "音频输入路由",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TpTextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pill Action Badge: 高级设置
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TpPurpleSoft,
                        modifier = Modifier
                            .clickable { onSelectMicClicked?.invoke() }
                            .testTag("btn_advanced_settings_pill")
                    ) {
                        Text(
                            text = if (strictBluetoothOnly) "严格蓝牙模式" else "高级设置",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TpPurplePrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onRefreshRoute,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_refresh_route")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新路由",
                            tint = TpTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Inner Device Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectMicClicked?.invoke() }
                    .testTag("btn_select_mic_row"),
                color = TpSurfaceElevated
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(TpPurpleSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExternalBt) Icons.Default.BluetoothConnected else if (routeInfo.isFallback) Icons.Default.Mic else Icons.Default.Bluetooth,
                            contentDescription = "设备图标",
                            tint = TpPurplePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = routeInfo.deviceName.ifEmpty { "内置麦克风" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TpTextPrimary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "当前启用输出: ${routeInfo.deviceName.ifEmpty { "内置麦克风" }}",
                            fontSize = 11.sp,
                            color = TpTextMuted,
                            maxLines = 1
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "切换麦克风",
                        tint = TpTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (strictBluetoothOnly && !isExternalBt) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(TpCoralSoft)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "警告",
                        tint = TpCoral,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "已启用严格蓝牙模式：录音前请先连接 DJI Mic Mini 发射器",
                        fontSize = 11.sp,
                        color = TpCoral,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
