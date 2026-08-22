package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioDeviceItem
import com.example.model.AudioFormatType
import com.example.model.RecordingConfig
import com.example.ui.theme.TpBgLight
import com.example.ui.theme.TpBorderLight
import com.example.ui.theme.TpBorderSubtle
import com.example.ui.theme.TpCoral
import com.example.ui.theme.TpGreen
import com.example.ui.theme.TpGreenDark
import com.example.ui.theme.TpGreenSoft
import com.example.ui.theme.TpPurpleLight
import com.example.ui.theme.TpPurplePrimary
import com.example.ui.theme.TpPurpleSecondary
import com.example.ui.theme.TpPurpleSoft
import com.example.ui.theme.TpSurfaceElevated
import com.example.ui.theme.TpSurfaceLight
import com.example.ui.theme.TpTextMuted
import com.example.ui.theme.TpTextPrimary
import com.example.ui.theme.TpTextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsBottomSheet(
    config: RecordingConfig,
    availableDevices: List<AudioDeviceItem>,
    onConfigChange: (RecordingConfig) -> Unit,
    onRefreshDevices: () -> Unit,
    storagePath: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TpBgLight,
        modifier = Modifier.testTag("settings_bottom_sheet")
    ) {
        SettingsContent(
            config = config,
            availableDevices = availableDevices,
            onConfigChange = onConfigChange,
            onRefreshDevices = onRefreshDevices,
            storagePath = storagePath,
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsContent(
    config: RecordingConfig,
    availableDevices: List<AudioDeviceItem>,
    onConfigChange: (RecordingConfig) -> Unit,
    onRefreshDevices: () -> Unit,
    storagePath: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TpBgLight)
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_scrollable_content")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TpPurpleSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "设置",
                        tint = TpPurplePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "录音配置与选项",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TpTextPrimary
                    )
                    Text(
                        text = "DJI Mic Mini 专调参数与格式控制",
                        fontSize = 11.sp,
                        color = TpTextMuted
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(TpSurfaceLight)
                    .border(1.dp, TpBorderLight, CircleShape)
                    .testTag("btn_close_settings")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = TpTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Microphone Device Selection Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(20.dp), spotColor = Color(0x08000000))
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, TpBorderLight, RoundedCornerShape(20.dp)),
            color = TpSurfaceLight
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "麦克风",
                            tint = TpPurplePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "录音麦克风输入源",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TpTextPrimary
                        )
                    }

                    IconButton(
                        onClick = onRefreshDevices,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("btn_refresh_mic_devices")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新设备列表",
                            tint = TpPurplePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "自动优先识别 DJI Mic Mini 及蓝牙 SCO 麦克风",
                    fontSize = 12.sp,
                    color = TpTextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (availableDevices.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = TpSurfaceElevated
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "正在扫描麦克风输入路由…",
                                    color = TpTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        availableDevices.forEach { device ->
                            val isSelected = config.preferredDeviceId == device.id
                            val isBt = device.isBluetooth
                            val icon = when {
                                device.id == 0 -> Icons.Default.AutoAwesome
                                isBt -> Icons.Default.Bluetooth
                                device.typeName.contains("USB", ignoreCase = true) -> Icons.Default.Usb
                                device.typeName.contains("耳机", ignoreCase = true) -> Icons.Default.Headset
                                else -> Icons.Default.Mic
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) TpPurplePrimary else TpBorderSubtle,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        onConfigChange(config.copy(preferredDeviceId = device.id))
                                    }
                                    .testTag("mic_device_item_${device.id}"),
                                color = if (isSelected) TpPurpleSoft.copy(alpha = 0.5f) else TpSurfaceElevated
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) TpPurpleSoft else Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = device.name,
                                            tint = if (isSelected) TpPurplePrimary else TpTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = device.name,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) TpPurplePrimary else TpTextPrimary
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = device.typeName,
                                                fontSize = 11.sp,
                                                color = TpTextMuted
                                            )
                                            if (isBt) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = TpGreenSoft
                                                ) {
                                                    Text(
                                                        text = "蓝牙/DJI",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TpGreenDark,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            onConfigChange(config.copy(preferredDeviceId = device.id))
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = TpPurplePrimary,
                                            unselectedColor = TpBorderLight
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Audio Format & Quality Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(20.dp), spotColor = Color(0x08000000))
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, TpBorderLight, RoundedCornerShape(20.dp)),
            color = TpSurfaceLight
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "音频格式",
                        tint = TpPurplePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "音频格式与音质参数",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TpTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "编码格式",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TpTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AudioFormatType.values().forEach { format ->
                        val isSelected = config.format == format
                        FilterChip(
                            selected = isSelected,
                            onClick = { onConfigChange(config.copy(format = format)) },
                            label = {
                                Text(
                                    text = format.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TpPurpleSoft,
                                selectedLabelColor = TpPurplePrimary,
                                containerColor = TpSurfaceElevated,
                                labelColor = TpTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) TpPurplePrimary else TpBorderLight
                            ),
                            modifier = Modifier.testTag("chip_format_${format.extension}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bitrate Selection for MP3 / AAC
                if (config.format != AudioFormatType.WAV) {
                    Text(
                        text = "编码比特率 (Bitrate)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TpTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(64, 128, 192, 256, 320).forEach { kbps ->
                            val isSelected = config.bitrateKbps == kbps
                            FilterChip(
                                selected = isSelected,
                                onClick = { onConfigChange(config.copy(bitrateKbps = kbps)) },
                                label = { Text("$kbps kbps", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TpPurpleSoft,
                                    selectedLabelColor = TpPurplePrimary,
                                    containerColor = TpSurfaceElevated,
                                    labelColor = TpTextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) TpPurplePrimary else TpBorderLight
                                ),
                                modifier = Modifier.testTag("chip_bitrate_$kbps")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Sample Rate
                Text(
                    text = "音频采样率 (Sample Rate)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TpTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "DJI Mic Mini 蓝牙 SCO 标准采样率为 16 kHz",
                    fontSize = 11.sp,
                    color = TpTextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(16_000, 44_100, 48_000).forEach { rate ->
                        val isSelected = config.sampleRate == rate
                        FilterChip(
                            selected = isSelected,
                            onClick = { onConfigChange(config.copy(sampleRate = rate)) },
                            label = { Text("${rate / 1000} kHz", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TpPurpleSoft,
                                selectedLabelColor = TpPurplePrimary,
                                containerColor = TpSurfaceElevated,
                                labelColor = TpTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) TpPurplePrimary else TpBorderLight
                            ),
                            modifier = Modifier.testTag("chip_rate_${rate}")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Segment Duration & Strategy Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(20.dp), spotColor = Color(0x08000000))
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, TpBorderLight, RoundedCornerShape(20.dp)),
            color = TpSurfaceLight
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "单段录音时长切割",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TpTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "到达设定时长自动无缝切割保存新分段，保障长录音文件安全",
                    fontSize = 12.sp,
                    color = TpTextMuted
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 30, 60, 120).forEach { mins ->
                        val isSelected = config.segmentDurationMinutes == mins
                        FilterChip(
                            selected = isSelected,
                            onClick = { onConfigChange(config.copy(segmentDurationMinutes = mins)) },
                            label = { Text("$mins 分钟", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TpPurpleSoft,
                                selectedLabelColor = TpPurplePrimary,
                                containerColor = TpSurfaceElevated,
                                labelColor = TpTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) TpPurplePrimary else TpBorderLight
                            ),
                            modifier = Modifier.testTag("chip_duration_$mins")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Strict Bluetooth Mode Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TpSurfaceElevated)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "严格蓝牙输入模式",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TpTextPrimary
                        )
                        Text(
                            text = "仅在连接 DJI Mic / 蓝牙麦克风时写入，断开即停止手机麦克风录入",
                            fontSize = 11.sp,
                            color = TpTextMuted,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = config.strictBluetoothOnly,
                        onCheckedChange = { checked ->
                            onConfigChange(config.copy(strictBluetoothOnly = checked))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TpPurplePrimary,
                            uncheckedThumbColor = TpTextMuted,
                            uncheckedTrackColor = TpBorderLight
                        ),
                        modifier = Modifier.testTag("switch_strict_bt")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Auto Gain / Noise Suppression Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TpSurfaceElevated)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "自动降噪与人声增益 (AGC)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TpTextPrimary
                        )
                        Text(
                            text = "启动硬件级回声消除与环境底噪滤除算法",
                            fontSize = 11.sp,
                            color = TpTextMuted,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = config.enableNoiseSuppression,
                        onCheckedChange = { checked ->
                            onConfigChange(config.copy(enableNoiseSuppression = checked))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TpPurplePrimary,
                            uncheckedThumbColor = TpTextMuted,
                            uncheckedTrackColor = TpBorderLight
                        ),
                        modifier = Modifier.testTag("switch_noise_suppression")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. File Storage Path Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(20.dp), spotColor = Color(0x08000000))
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, TpBorderLight, RoundedCornerShape(20.dp)),
            color = TpSurfaceLight
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(TpPurpleSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "文件目录",
                        tint = TpPurplePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "录音保存目录",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TpTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = storagePath,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TpTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. About Banner
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(TpPurpleSoft),
            color = TpPurpleSoft
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TpPurplePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TP",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "TP RECORDER · v1.0.0",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TpPurplePrimary
                    )
                    Text(
                        text = "针对 DJI Mic Mini 发射器与蓝牙 SCO 音频流深度调优",
                        fontSize = 11.sp,
                        color = TpTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Done button
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_settings_done"),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TpPurplePrimary,
                contentColor = Color.White
            )
        ) {
            Text("完成配置", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
