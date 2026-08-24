package com.dji.recorder.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dji.recorder.R
import com.dji.recorder.model.AppThemeMode
import com.dji.recorder.model.AudioConfig
import com.dji.recorder.model.AudioFormatType
import com.dji.recorder.model.NoiseReductionMode
import com.dji.recorder.model.RecorderStatus
import com.dji.recorder.model.RecordingItem
import com.dji.recorder.ui.components.AudioSettingsDialog
import com.dji.recorder.ui.components.NoiseReductionBottomSheet
import com.dji.recorder.ui.components.WaveformVisualizer
import com.dji.recorder.ui.theme.DjiGreen
import com.dji.recorder.ui.theme.DjiRed
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DjiRecorderScreen(viewModel: DjiRecorderViewModel) {
    val context = LocalContext.current

    val permissionsToRequest = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val permissionsState = rememberMultiplePermissionsState(permissions = permissionsToRequest)

    val status by viewModel.status.collectAsState()
    val connectedMic by viewModel.connectedMic.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val amplitudeHistory by viewModel.amplitudeHistory.collectAsState()
    val currentDecibels by viewModel.currentDecibels.collectAsState()
    val noiseReductionMode by viewModel.noiseReductionMode.collectAsState()
    val audioConfig by viewModel.audioConfig.collectAsState()
    val activeRoutedDevice by viewModel.activeRoutedDevice.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val progress by viewModel.processingProgress.collectAsState()
    val recordings by viewModel.recordingsList.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPlayingFile by viewModel.currentPlayingFile.collectAsState()
    val playbackProgress by viewModel.playbackProgress.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var showNoiseReductionSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    if (showNoiseReductionSheet) {
        NoiseReductionBottomSheet(
            currentMode = noiseReductionMode,
            onSelectMode = { viewModel.setNoiseReductionMode(it) },
            onDismiss = { showNoiseReductionSheet = false }
        )
    }

    if (showSettingsDialog) {
        AudioSettingsDialog(
            currentConfig = audioConfig,
            onSaveConfig = { viewModel.updateAudioConfig(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (!permissionsState.allPermissionsGranted) {
            PermissionRequiredView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onRequestPermissions = { permissionsState.launchMultiplePermissionRequest() }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶栏（品牌名称 + 主题切换 + 设置齿轮）
                TopHeaderBar(
                    themeMode = themeMode,
                    onToggleTheme = { viewModel.toggleTheme() },
                    onOpenSettings = { showSettingsDialog = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // DJI 硬件设备状态卡片
                DjiHardwareCard(
                    status = status,
                    deviceName = connectedMic?.name,
                    activeRoutedDevice = activeRoutedDevice,
                    noiseMode = noiseReductionMode,
                    audioConfig = audioConfig,
                    onRefresh = { viewModel.scanForBluetoothMic() },
                    onOpenSettings = { showSettingsDialog = true }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 录音计时器与转码/AI处理进度
                TimerDisplay(
                    durationMs = durationMs,
                    status = status,
                    progress = progress
                )

                Spacer(modifier = Modifier.height(12.dp))

                WaveformVisualizer(
                    amplitudes = amplitudeHistory,
                    currentDecibels = currentDecibels,
                    isRecording = status == RecorderStatus.RECORDING
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 录音历史列表
                RecordingsListSection(
                    recordings = recordings,
                    isPlaying = isPlaying,
                    playingFilePath = currentPlayingFile?.absolutePath,
                    playbackProgress = playbackProgress,
                    onPlay = { viewModel.playRecording(it) },
                    onStop = { viewModel.stopPlayback() },
                    onDelete = { viewModel.deleteRecording(it) },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 底部操作栏
                BottomControlBar(
                    status = status,
                    noiseMode = noiseReductionMode,
                    onOpenNoiseSheet = { showNoiseReductionSheet = true },
                    onStart = { viewModel.startRecording() },
                    onStop = { viewModel.stopRecording() }
                )
            }
        }
    }
}

@Composable
fun TopHeaderBar(
    themeMode: AppThemeMode,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Column {
                Text(
                    text = "DJI MIC STUDIO",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Bluetooth Mic HD Audio System",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = when (themeMode) {
                        AppThemeMode.LIGHT -> Icons.Default.Brightness7
                        AppThemeMode.DARK -> Icons.Default.Brightness4
                        AppThemeMode.SYSTEM -> Icons.Default.Brightness4
                    },
                    contentDescription = "Toggle Theme",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun DjiHardwareCard(
    status: RecorderStatus,
    deviceName: String?,
    activeRoutedDevice: String,
    noiseMode: NoiseReductionMode,
    audioConfig: AudioConfig,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isConnected = deviceName != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.dji_mic),
                        contentDescription = "DJI Mic",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = deviceName ?: "未检测到蓝牙麦克风",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) DjiGreen else DjiRed)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = if (status == RecorderStatus.RECORDING) activeRoutedDevice else if (isConnected) "强制音频输入锁定 • 48kHz" else "请开启蓝牙配对外部麦克风",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isConnected) DjiGreen else DjiRed
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.clickable { onOpenSettings() }
                        ) {
                            val formatText = when (audioConfig.format) {
                                AudioFormatType.WAV -> "WAV • ${audioConfig.sampleRate / 1000}kHz 无损"
                                AudioFormatType.MP3 -> "MP3 • ${audioConfig.bitrateKbps}kbps"
                                AudioFormatType.AAC -> "AAC • ${audioConfig.bitrateKbps}kbps"
                            }
                            Text(
                                text = formatText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "降噪: ${noiseMode.badge}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun TimerDisplay(
    durationMs: Long,
    status: RecorderStatus,
    progress: Float
) {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val millis = (durationMs % 1000) / 10

    val timeString = String.format(Locale.getDefault(), "%02d:%02d:%02d.%02d", hours, minutes, seconds, millis)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when (status) {
            RecorderStatus.PROCESSING_AI -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "DPDFNet2 HR 降噪处理中 ${(progress * 100).toInt()}%...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            RecorderStatus.ENCODING -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "音频硬件转码中 ${(progress * 100).toInt()}%...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            else -> {
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (status == RecorderStatus.RECORDING) DjiRed else MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun BottomControlBar(
    status: RecorderStatus,
    noiseMode: NoiseReductionMode,
    onOpenNoiseSheet: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val isRecording = status == RecorderStatus.RECORDING
    val isReady = status == RecorderStatus.READY || status == RecorderStatus.SAVED

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .clickable(enabled = !isRecording) { onOpenNoiseSheet() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (noiseMode) {
                        NoiseReductionMode.AI_HIGH -> Icons.Default.AutoAwesome
                        NoiseReductionMode.FAST_LOW -> Icons.Default.Hearing
                        NoiseReductionMode.OFF -> Icons.Default.Mic
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "降噪 • ${noiseMode.badge}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isRecording -> DjiRed
                        isReady -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .clickable(enabled = isReady || isRecording) {
                    if (isRecording) onStop() else onStart()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop" else "Record",
                tint = if (isRecording || isReady) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
fun RecordingsListSection(
    recordings: List<RecordingItem>,
    isPlaying: Boolean,
    playingFilePath: String?,
    playbackProgress: Float,
    onPlay: (RecordingItem) -> Unit,
    onStop: () -> Unit,
    onDelete: (RecordingItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "录音列表 (${recordings.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无录音文件",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recordings, key = { it.fileName }) { item ->
                    val isCurrentPlaying = isPlaying && playingFilePath == item.file.absolutePath

                    RecordingItemRow(
                        item = item,
                        isCurrentPlaying = isCurrentPlaying,
                        playbackProgress = if (isCurrentPlaying) playbackProgress else 0f,
                        onPlayClick = {
                            if (isCurrentPlaying) onStop() else onPlay(item)
                        },
                        onDeleteClick = { onDelete(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun RecordingItemRow(
    item: RecordingItem,
    isCurrentPlaying: Boolean,
    playbackProgress: Float,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val durationSec = item.durationMs / 1000
    val durationText = String.format(Locale.getDefault(), "%02d:%02d", durationSec / 60, durationSec % 60)
    val sizeMb = String.format(Locale.getDefault(), "%.2f MB", item.sizeBytes / (1024.0 * 1024.0))
    val dateText = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(item.createdAt))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrentPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isCurrentPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = if (isCurrentPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Play/Stop",
                        tint = if (isCurrentPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.fileName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = item.file.extension.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "$durationText • $sizeMb • $dateText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isCurrentPlaying) {
                LinearProgressIndicator(
                    progress = { playbackProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun PermissionRequiredView(
    modifier: Modifier = Modifier,
    onRequestPermissions: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.dji_mic),
            contentDescription = null,
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "需要录音与蓝牙权限",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "该应用专为蓝牙外部麦克风（如 DJI Mic）设计，需要获取音频录制和蓝牙连接权限以识别外接麦克风设备。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermissions,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("授权并继续", fontWeight = FontWeight.Bold)
        }
    }
}
