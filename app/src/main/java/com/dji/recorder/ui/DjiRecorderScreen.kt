package com.dji.recorder.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.scale
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
import com.dji.recorder.ui.theme.NeoAcidLime
import com.dji.recorder.ui.theme.NeoBadge
import com.dji.recorder.ui.theme.NeoBlack
import com.dji.recorder.ui.theme.NeoButton
import com.dji.recorder.ui.theme.NeoCard
import com.dji.recorder.ui.theme.NeoCyberYellow
import com.dji.recorder.ui.theme.NeoElectricCyan
import com.dji.recorder.ui.theme.NeoHotRed
import com.dji.recorder.ui.theme.NeoLavender
import com.dji.recorder.ui.theme.NeoWhite
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
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶栏
                TopHeaderBar(
                    themeMode = themeMode,
                    onToggleTheme = { viewModel.toggleTheme() },
                    onOpenSettings = { showSettingsDialog = true }
                )

                Spacer(modifier = Modifier.height(14.dp))

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

                // 录音计时器与转码/AI处理进度（带呼吸红光晕圈动效）
                TimerDisplay(
                    durationMs = durationMs,
                    status = status,
                    progress = progress
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 实时声波可视化
                WaveformVisualizer(
                    amplitudes = amplitudeHistory,
                    currentDecibels = currentDecibels,
                    isRecording = status == RecorderStatus.RECORDING
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 录音历史列表 (带动态跳动音轨动画)
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

                Spacer(modifier = Modifier.height(14.dp))

                // 底部操作栏（带机械下沉按压动效）
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
    val borderColor = MaterialTheme.colorScheme.outline

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(38.dp)
                )
            }
            Column {
                Text(
                    text = "DJI MIC STUDIO",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "PRO AUDIO ENGINE • v1.0",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // 主题切换按键
            AnimatedIconButton(
                backgroundColor = NeoCyberYellow,
                borderColor = borderColor,
                onClick = onToggleTheme
            ) {
                Icon(
                    imageVector = when (themeMode) {
                        AppThemeMode.LIGHT -> Icons.Default.Brightness7
                        AppThemeMode.DARK -> Icons.Default.Brightness4
                        AppThemeMode.SYSTEM -> Icons.Default.Brightness4
                    },
                    contentDescription = "Toggle Theme",
                    tint = NeoBlack,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 设置按键
            AnimatedIconButton(
                backgroundColor = NeoAcidLime,
                borderColor = borderColor,
                onClick = onOpenSettings
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = NeoBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AnimatedIconButton(
    backgroundColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animOffset by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btnOffset"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .offset(x = animOffset, y = animOffset)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
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
    val borderColor = MaterialTheme.colorScheme.outline

    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = borderColor,
        padding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isConnected) NeoAcidLime.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, borderColor, RoundedCornerShape(10.dp))
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
                            text = deviceName ?: "未检测到麦克风",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isConnected) NeoAcidLime else NeoHotRed)
                                .border(1.dp, borderColor, RoundedCornerShape(2.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = if (status == RecorderStatus.RECORDING) activeRoutedDevice else if (isConnected) "⚡ 硬件音频直通锁定 • 48kHz" else "请开启蓝牙配对外部麦克风",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isConnected) (if (status == RecorderStatus.RECORDING) NeoHotRed else NeoAcidLime) else NeoHotRed
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val formatText = when (audioConfig.format) {
                            AudioFormatType.WAV -> "WAV • ${audioConfig.sampleRate / 1000}kHz"
                            AudioFormatType.MP3 -> "MP3 • ${audioConfig.bitrateKbps}k"
                            AudioFormatType.AAC -> "AAC • ${audioConfig.bitrateKbps}k"
                        }
                        NeoBadge(
                            text = formatText,
                            backgroundColor = NeoCyberYellow,
                            textColor = NeoBlack,
                            onClick = onOpenSettings
                        )

                        NeoBadge(
                            text = "降噪: ${noiseMode.badge}",
                            backgroundColor = NeoLavender,
                            textColor = NeoBlack,
                            onClick = onOpenSettings
                        )
                    }
                }
            }

            AnimatedIconButton(
                backgroundColor = NeoElectricCyan,
                borderColor = borderColor,
                onClick = onRefresh
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = NeoBlack,
                    modifier = Modifier.size(20.dp)
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
    val isRecording = status == RecorderStatus.RECORDING
    val borderColor = MaterialTheme.colorScheme.outline

    // 呼吸脉冲动效
    val infiniteTransition = rememberInfiniteTransition(label = "recPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRecording) 1.35f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        // 底层实体硬阴影
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
                .border(2.5.dp, Color.Black, RoundedCornerShape(14.dp))
        )

        // 表层 LCD 仪表卡片
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(2.5.dp, if (isRecording) NeoHotRed else borderColor, RoundedCornerShape(14.dp))
                .padding(vertical = 16.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (status) {
                RecorderStatus.PROCESSING_AI -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.5.dp,
                            color = NeoAcidLime
                        )
                        Text(
                            text = "DEEPFILTERNET 3 ENHANCING...",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.5.dp, borderColor, RoundedCornerShape(4.dp)),
                        color = NeoAcidLime,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
                RecorderStatus.ENCODING -> {
                    Text(
                        text = "LAME MP3 CONVERTING...",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.5.dp, borderColor, RoundedCornerShape(4.dp)),
                        color = NeoCyberYellow,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
                else -> {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        ),
                        color = if (isRecording) NeoHotRed else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(if (isRecording) pulseScale else 1f)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isRecording) NeoHotRed else NeoAcidLime)
                                .border(1.dp, borderColor, RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = if (isRecording) "REC LIVE • 48000 HZ" else "STANDBY • READY TO CAPTURE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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
    val borderColor = MaterialTheme.colorScheme.outline

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SAVED MASTER TAPES",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            NeoBadge(
                text = "${recordings.size} ITEMS",
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(2.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "暂无录音文件",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 6.dp)
            ) {
                items(recordings, key = { it.file.absolutePath }) { item ->
                    val isCurrent = isPlaying && playingFilePath == item.file.absolutePath
                    RecordingItemRow(
                        item = item,
                        isCurrentPlaying = isCurrent,
                        playbackProgress = if (isCurrent) playbackProgress else 0f,
                        onPlay = { onPlay(item) },
                        onStop = onStop,
                        onDelete = { onDelete(item) }
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
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(item.createdAt))
    val sizeMb = String.format(Locale.getDefault(), "%.1fMB", item.sizeBytes / (1024f * 1024f))
    val durationSec = item.durationMs / 1000
    val durationStr = String.format(Locale.getDefault(), "%02d:%02d", durationSec / 60, durationSec % 60)
    val borderColor = MaterialTheme.colorScheme.outline

    Box(modifier = Modifier.fillMaxWidth()) {
        // 阴影
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black)
                .border(2.dp, Color.Black, RoundedCornerShape(10.dp))
        )

        // 卡片
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy))
                .clip(RoundedCornerShape(10.dp))
                .background(if (isCurrentPlaying) NeoAcidLime.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                .border(2.dp, borderColor, RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCurrentPlaying) NeoHotRed else NeoAcidLime)
                            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
                            .clickable { if (isCurrentPlaying) onStop() else onPlay() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCurrentPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isCurrentPlaying) "Stop" else "Play",
                            tint = if (isCurrentPlaying) NeoWhite else NeoBlack,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.file.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            if (isCurrentPlaying) {
                                AnimatedEqualizerBars()
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "$dateStr • $durationStr • $sizeMb",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NeoBadge(
                        text = item.formatType.name,
                        backgroundColor = NeoCyberYellow,
                        textColor = NeoBlack
                    )

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.5.dp, borderColor, RoundedCornerShape(6.dp))
                            .clickable { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = NeoHotRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (isCurrentPlaying) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { playbackProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(3.dp)),
                    color = NeoAcidLime,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

/**
 * 播放中动态跳动的 3 条音轨柱动画
 */
@Composable
fun AnimatedEqualizerBars() {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 12f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(280, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(14.dp)
    ) {
        Box(modifier = Modifier.width(2.5.dp).height(h1.dp).background(NeoAcidLime))
        Box(modifier = Modifier.width(2.5.dp).height(h2.dp).background(NeoCyberYellow))
        Box(modifier = Modifier.width(2.5.dp).height(h3.dp).background(NeoHotRed))
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
    val borderColor = MaterialTheme.colorScheme.outline

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 降噪切换按钮 (NeoButton 风格)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val animOffset by animateDpAsState(
                targetValue = if (isPressed) 2.5.dp else 0.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "noiseBtnOffset"
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 3.5.dp, y = 3.5.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = animOffset, y = animOffset)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeoLavender)
                    .border(2.5.dp, borderColor, RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onOpenNoiseSheet() }
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Hearing,
                    contentDescription = null,
                    tint = NeoBlack,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "降噪: ${noiseMode.badge}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    ),
                    color = NeoBlack
                )
            }
        }

        // 核心录音触发大按钮 (START / STOP)
        Box(
            modifier = Modifier
                .weight(1.4f)
                .height(56.dp)
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val animOffset by animateDpAsState(
                targetValue = if (isPressed) 3.dp else 0.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "recBtnOffset"
            )

            val btnBg by animateColorAsState(
                targetValue = if (isRecording) NeoHotRed else NeoAcidLime,
                animationSpec = tween(300),
                label = "btnBgColor"
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 3.5.dp, y = 3.5.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = animOffset, y = animOffset)
                    .clip(RoundedCornerShape(12.dp))
                    .background(btnBg)
                    .border(2.5.dp, borderColor, RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { if (isRecording) onStop() else onStart() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = isRecording,
                    transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                    },
                    label = "btnContent"
                ) { rec ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (rec) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (rec) "Stop" else "Record",
                            tint = if (rec) NeoWhite else NeoBlack,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (rec) "STOP RECORD" else "REC NOW",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = if (rec) NeoWhite else NeoBlack
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRequiredView(
    modifier: Modifier = Modifier,
    onRequestPermissions: () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outline

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surface,
            borderColor = borderColor,
            padding = 24.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(NeoCyberYellow)
                        .border(2.5.dp, borderColor, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = NeoBlack,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "PERMISSIONS REQUIRED",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "本应用专为 DJI Mic 蓝牙与 USB 高清声卡定制，需要麦克风录音与附近蓝牙设备扫描权限。",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                NeoButton(
                    onClick = onRequestPermissions,
                    backgroundColor = NeoAcidLime,
                    borderColor = borderColor
                ) {
                    Text(
                        text = "GRANT PERMISSIONS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = NeoBlack
                    )
                }
            }
        }
    }
}
