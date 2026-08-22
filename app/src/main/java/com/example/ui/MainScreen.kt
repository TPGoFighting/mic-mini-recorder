package com.example.ui

import android.content.Intent
import android.os.StatFs
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.audio.AudioUtils
import com.example.model.AudioRouteInfo
import com.example.model.RecordingFileItem
import com.example.model.RecordingState
import com.example.model.RecordingConfig
import com.example.service.RecordingServiceUiState
import com.example.ui.components.SettingsBottomSheet
import com.example.ui.theme.TpBgLight
import com.example.ui.theme.TpBorderLight
import com.example.ui.theme.TpBorderSubtle
import com.example.ui.theme.TpCoral
import com.example.ui.theme.TpCoralSoft
import com.example.ui.theme.TpGreen
import com.example.ui.theme.TpGreenSoft
import com.example.ui.theme.TpPurplePrimary
import com.example.ui.theme.TpPurpleSoft
import com.example.ui.theme.TpSurfaceElevated
import com.example.ui.theme.TpSurfaceLight
import com.example.ui.theme.TpTextMuted
import com.example.ui.theme.TpTextPrimary
import com.example.ui.theme.TpTextSecondary
import com.example.viewmodel.MainViewModel
import java.util.Locale

private enum class AppPage { CONNECT, RECORD, SESSIONS, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val serviceState by viewModel.serviceState.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val recordings by viewModel.recordings.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val idleRouteInfo by viewModel.idleRouteInfo.collectAsStateWithLifecycle()
    val availableDevices by viewModel.availableDevices.collectAsStateWithLifecycle()
    val showSettings by viewModel.showSettingsSheet.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(AppPage.RECORD.name) }
    val isBusy = serviceState.state in setOf(RecordingState.RECORDING, RecordingState.PAUSED, RecordingState.SEARCHING_BLUETOOTH)
    val activeRoute = if (isBusy) serviceState.routeInfo else idleRouteInfo

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TpBgLight,
        bottomBar = {
            if (page != AppPage.CONNECT.name) {
                MainBottomBar(page, recordings.size) { page = it }
            }
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues).statusBarsPadding()) {
            when (page) {
                AppPage.CONNECT.name -> ConnectPage(
                    onOpenBluetoothSettings = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                    onRescan = { viewModel.refreshRouteInfo(); page = AppPage.RECORD.name },
                    onBack = { page = AppPage.RECORD.name }
                )
                AppPage.SESSIONS.name -> SessionsPage(
                    recordings = recordings,
                    playingPath = playerState.currentFilePath,
                    isPlaying = playerState.isPlaying,
                    onPlay = viewModel::playRecording,
                    onShare = { item -> viewModel.getShareIntent(item)?.let(context::startActivity) },
                    onRefresh = viewModel::refreshRecordings
                )
                AppPage.SETTINGS.name -> SettingsPage(
                    config = config,
                    onBack = { page = AppPage.RECORD.name },
                    onOpenAdvanced = { viewModel.setShowSettings(true) },
                    onOpenInput = { page = AppPage.CONNECT.name }
                )
                else -> RecordPage(
                    serviceState = serviceState,
                    routeInfo = activeRoute,
                    config = config,
                    onStart = viewModel::startRecording,
                    onStop = viewModel::stopRecording,
                    onPause = viewModel::pauseRecording,
                    onResume = viewModel::resumeRecording,
                    onRefresh = viewModel::refreshRouteInfo,
                    onOpenConnect = { page = AppPage.CONNECT.name },
                    onOpenSettings = { page = AppPage.SETTINGS.name }
                )
            }

            if (showSettings) {
                val storagePath = context.getExternalFilesDir("Music/MicMini")?.absolutePath ?: "Music/MicMini"
                SettingsBottomSheet(
                    config = config,
                    availableDevices = availableDevices,
                    onConfigChange = viewModel::updateConfig,
                    onRefreshDevices = viewModel::refreshRouteInfo,
                    storagePath = storagePath,
                    onDismiss = { viewModel.setShowSettings(false) }
                )
            }
        }
    }
}

@Composable
private fun AppHeader(title: String = "TP Recorder", showBack: Boolean = false, onBack: (() -> Unit)? = null, onMore: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (showBack) {
            IconButton(onClick = { onBack?.invoke() }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TpPurplePrimary)
            }
        } else {
            Image(painterResource(R.drawable.tp_signal_logo), "Signal TP", Modifier.size(42.dp), contentScale = ContentScale.Fit)
        }
        Text(title, color = TpTextPrimary, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.5).sp)
        if (onMore != null) {
            IconButton(onClick = onMore, modifier = Modifier.size(38.dp)) { Icon(Icons.Default.MoreVert, "更多", tint = TpTextPrimary) }
        } else {
            Spacer(Modifier.size(38.dp))
        }
    }
}

@Composable
private fun ConnectPage(onOpenBluetoothSettings: () -> Unit, onRescan: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppHeader(onMore = onBack)
        Spacer(Modifier.height(62.dp))
        Image(painterResource(R.drawable.tp_signal_logo), "Signal TP Logo", Modifier.size(126.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.height(22.dp))
        Text("连接你的外部麦克风", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TpTextPrimary)
        Spacer(Modifier.height(10.dp))
        Text("在系统蓝牙设置中连接 DJI Mic Mini 发射器", color = TpTextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(26.dp))
        PrimaryButton("打开蓝牙设置", onClick = onOpenBluetoothSettings)
        Text("我已连接，重新扫描", color = TpTextSecondary, fontSize = 13.sp, modifier = Modifier.padding(14.dp).clickable(onClick = onRescan))
        Spacer(Modifier.height(86.dp))
        Surface(shape = RoundedCornerShape(12.dp), color = TpSurfaceElevated, border = androidx.compose.foundation.BorderStroke(1.dp, TpBorderLight)) {
            Text("只有系统确认的蓝牙 SCO / BLE Headset 输入才会开始录音。不会回退到手机麦克风。", color = TpTextMuted, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(14.dp))
        }
    }
}

@Composable
private fun RecordPage(
    serviceState: RecordingServiceUiState,
    routeInfo: AudioRouteInfo,
    config: RecordingConfig,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRefresh: () -> Unit,
    onOpenConnect: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isRecording = serviceState.state == RecordingState.RECORDING
    val isPaused = serviceState.state == RecordingState.PAUSED
    val isSearching = serviceState.state == RecordingState.SEARCHING_BLUETOOTH
    val hasExternalInput = routeInfo.isExternalBluetooth
    val statusTone = when { isRecording -> TpCoral; hasExternalInput -> TpGreen; isSearching -> Color(0xFFB88427); else -> TpCoral }
    val statusText = when { isRecording -> "正在录音"; isPaused -> "已暂停 · 当前分段已保存"; isSearching -> "等待蓝牙重新连接"; hasExternalInput -> "已准备好"; else -> "未连接外部麦克风" }
    val primaryText = when { isRecording -> "结束并保存"; isPaused -> "继续录音"; isSearching -> "停止并保存已有分段"; else -> "开始录音" }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AppHeader(onMore = onOpenConnect)
        RouteCard(routeInfo, isRecording, statusTone, if (hasExternalInput) onRefresh else onOpenConnect)
        Spacer(Modifier.height(16.dp))
        Text(AudioUtils.formatDuration(serviceState.elapsedDurationMs), color = TpTextPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 48.sp, letterSpacing = (-2).sp, modifier = Modifier.testTag("recording_timer_text"))
        Text(statusText, color = statusTone, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        LiveSignalPanel(serviceState.recentWaveform, serviceState.currentRmsDb, isRecording)
        Spacer(Modifier.height(16.dp))
        PrimaryButton(primaryText, isRecording || isPaused || isSearching || hasExternalInput, if (isRecording) Icons.Default.Stop else if (isPaused) Icons.Default.PlayArrow else Icons.Default.Mic, when { isRecording -> onStop; isPaused -> onResume; isSearching -> onStop; else -> onStart })
        if (isRecording) SecondaryButton("暂停", Icons.Default.Pause, onPause)
        else if (isSearching) SecondaryButton("重新扫描", Icons.Default.Refresh, onRefresh)
        else SecondaryButton("设置", Icons.Default.Settings, onOpenSettings)
        Spacer(Modifier.height(12.dp))
        RecordingSummary(config)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun RouteCard(routeInfo: AudioRouteInfo, isRecording: Boolean, tone: Color, onClick: () -> Unit) {
    val isExternal = routeInfo.isExternalBluetooth
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        RoundedCornerShape(16.dp),
        when { isRecording -> TpCoralSoft.copy(alpha = 0.3f); isExternal -> TpGreenSoft.copy(alpha = 0.35f); else -> Color(0xFFF3E7C7).copy(alpha = 0.5f) },
        border = androidx.compose.foundation.BorderStroke(1.dp, tone.copy(alpha = 0.55f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("外部输入", color = TpTextSecondary, fontSize = 12.sp); Text("Bluetooth", color = TpTextMuted, fontSize = 12.sp) }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isExternal) {
                    Image(painterResource(R.drawable.dji_mic_mini), "DJI Mic Mini", Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(tone), contentAlignment = Alignment.Center) { Icon(if (isRecording) Icons.Default.GraphicEq else Icons.Default.Bluetooth, null, tint = Color.White) }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (isExternal) routeInfo.deviceName.ifBlank { "DJI Mic Mini" } else "未连接", color = TpTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(if (isExternal) "蓝牙 SCO · 已确认可录音" else routeInfo.description, color = tone, fontSize = 13.sp)
                }
                Icon(if (isExternal) Icons.Default.Check else Icons.Default.Refresh, null, tint = tone, modifier = Modifier.size(27.dp))
            }
        }
    }
}

@Composable
private fun LiveSignalPanel(waveform: List<Float>, rmsDb: Float, isRecording: Boolean) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), TpSurfaceLight, border = androidx.compose.foundation.BorderStroke(1.dp, TpBorderLight)) {
        Column(Modifier.padding(13.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("实时电平", color = TpTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(if (isRecording) String.format(Locale.US, "%.0f dBFS", rmsDb) else "-- dBFS", color = if (rmsDb > -6f) TpCoral else TpTextSecondary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Canvas(Modifier.fillMaxWidth().height(72.dp)) {
                val count = waveform.size.coerceAtLeast(1)
                val barWidth = (size.width / count * 0.6f).coerceAtLeast(2f)
                waveform.forEachIndexed { index, amplitude ->
                    val height = if (isRecording) size.height * amplitude.coerceIn(0.04f, 0.95f) else 2f
                    val x = index * (size.width / count)
                    drawLine(if (isRecording) TpTextPrimary else TpBorderLight, androidx.compose.ui.geometry.Offset(x, size.height / 2 - height / 2), androidx.compose.ui.geometry.Offset(x, size.height / 2 + height / 2), barWidth, StrokeCap.Round)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf("-60", "-48", "-36", "-24", "-12", "0").forEach { Text(it, color = TpTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace) } }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(34) { index ->
                    Box(Modifier.weight(1f).height(9.dp).clip(RoundedCornerShape(2.dp)).background(when { isRecording && index < 23 -> TpGreen; isRecording && index < 28 -> Color(0xFFB88427); else -> TpBorderSubtle }))
                }
            }
        }
    }
}

@Composable
private fun RecordingSummary(config: RecordingConfig) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), TpSurfaceElevated, border = androidx.compose.foundation.BorderStroke(1.dp, TpBorderLight)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            SummaryRow(Icons.Default.Mic, "${config.format.displayName} · ${config.sampleRate / 1000} kHz · ${config.segmentDurationMinutes} 分钟自动分段")
            SummaryRow(Icons.Default.Folder, "存储位置：内部存储 / TP_Recorder")
            SummaryRow(Icons.Default.AccessTime, "可用空间：${availableStorageLabel()}")
        }
    }
}

@Composable
private fun SummaryRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(TpBorderSubtle), contentAlignment = Alignment.Center) { Icon(icon, null, tint = TpTextSecondary, modifier = Modifier.size(15.dp)) }
        Spacer(Modifier.width(9.dp))
        Text(text, color = TpTextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun SessionsPage(recordings: List<RecordingFileItem>, playingPath: String?, isPlaying: Boolean, onPlay: (RecordingFileItem) -> Unit, onShare: (RecordingFileItem) -> Unit, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
        AppHeader(title = "会话库", onMore = onRefresh)
        Text(if (recordings.isEmpty()) "最近会话" else "今天 · ${recordings.size} 个会话", color = TpTextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp, bottom = 12.dp))
        if (recordings.isEmpty()) {
            Surface(Modifier.fillMaxWidth().height(270.dp), RoundedCornerShape(16.dp), TpSurfaceLight, border = androidx.compose.foundation.BorderStroke(1.dp, TpBorderLight)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Folder, null, tint = TpTextMuted, modifier = Modifier.size(46.dp))
                    Spacer(Modifier.height(10.dp)); Text("会话库为空", color = TpTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("连接 DJI Mic Mini 后，第一段录音会出现在这里。", color = TpTextMuted, fontSize = 12.sp)
                }
            }
        } else {
            recordings.forEach { item ->
                SessionRow(item, isPlaying && item.path == playingPath, { onPlay(item) }, { onShare(item) })
                Spacer(Modifier.height(9.dp))
            }
        }
    }
}

@Composable
private fun SessionRow(item: RecordingFileItem, isPlaying: Boolean, onPlay: () -> Unit, onShare: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), TpSurfaceLight, border = androidx.compose.foundation.BorderStroke(1.dp, TpBorderLight)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(TpPurplePrimary), contentAlignment = Alignment.Center) { Icon(Icons.Default.GraphicEq, null, tint = Color.White, modifier = Modifier.size(19.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, color = TpTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${item.formattedDuration} · ${item.formattedSize} · MP3", color = TpTextSecondary, fontSize = 11.sp)
                Text("DJI Mic Mini · READY", color = TpGreen, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onPlay, modifier = Modifier.size(34.dp)) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "播放", tint = TpPurplePrimary) }
                IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Share, "导出", tint = TpGreen) }
            }
        }
    }
}

@Composable
private fun SettingsPage(config: RecordingConfig, onBack: () -> Unit, onOpenAdvanced: () -> Unit, onOpenInput: () -> Unit) {
    val rows = listOf(
        Triple(Icons.Default.Mic, "输入设备", "DJI Mic Mini"),
        Triple(Icons.Default.GraphicEq, "录音格式", config.format.displayName),
        Triple(Icons.Default.GraphicEq, "音频质量", "${config.sampleRate / 1000} kHz · ${config.bitrateKbps} kbps"),
        Triple(Icons.Default.AccessTime, "自动分段", "${config.segmentDurationMinutes} 分钟"),
        Triple(Icons.Default.Folder, "存储", "设备存储"),
        Triple(Icons.Default.Headset, "后台录音", "前台服务已开启"),
        Triple(Icons.Default.Info, "关于", "")
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
        AppHeader(title = "设置", showBack = true, onBack = onBack)
        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), TpSurfaceLight, border = androidx.compose.foundation.BorderStroke(1.dp, TpBorderLight)) {
            Column {
                rows.forEachIndexed { index, (icon, title, value) ->
                    Row(Modifier.fillMaxWidth().clickable { if (title == "输入设备") onOpenInput() else onOpenAdvanced() }.padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, tint = TpPurplePrimary, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(13.dp)); Text(title, color = TpTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); Text(value, color = TpTextMuted, fontSize = 12.sp); Text("›", color = TpTextMuted, fontSize = 26.sp, modifier = Modifier.padding(start = 7.dp))
                    }
                    if (index < rows.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(TpBorderSubtle))
                }
            }
        }
        Spacer(Modifier.height(18.dp)); Text("仅外部蓝牙输入 · 本地存储 · MP3 优先", color = TpTextMuted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun MainBottomBar(page: String, recordingCount: Int, onNavigate: (String) -> Unit) {
    Surface(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp).shadow(8.dp, RoundedCornerShape(22.dp)), RoundedCornerShape(22.dp), TpSurfaceLight, border = androidx.compose.foundation.BorderStroke(1.dp, TpBorderLight)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 5.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            BottomBarItem("会话库", Icons.AutoMirrored.Filled.List, AppPage.SESSIONS.name, page, recordingCount, onNavigate)
            BottomBarItem("录音", Icons.Default.Mic, AppPage.RECORD.name, page, 0, onNavigate)
            BottomBarItem("设置", Icons.Default.Settings, AppPage.SETTINGS.name, page, 0, onNavigate)
        }
    }
}

@Composable
private fun BottomBarItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, target: String, current: String, badge: Int, onNavigate: (String) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(14.dp)).background(if (target == current) TpPurpleSoft else Color.Transparent).clickable { onNavigate(target) }.padding(horizontal = 13.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, label, tint = if (target == current) TpPurplePrimary else TpTextMuted, modifier = Modifier.size(19.dp)); Spacer(Modifier.width(5.dp)); Text(label, color = if (target == current) TpPurplePrimary else TpTextMuted, fontSize = 12.sp, fontWeight = if (target == current) FontWeight.Bold else FontWeight.Normal); if (badge > 0) Text("  $badge", color = TpGreen, fontSize = 10.sp)
    }
}

@Composable
private fun PrimaryButton(label: String, enabled: Boolean = true, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(58.dp).testTag("primary_record_button"), shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = TpPurplePrimary, contentColor = Color.White, disabledContainerColor = TpBorderLight, disabledContentColor = TpTextMuted)) {
        if (icon != null) { Icon(icon, null, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(10.dp)) }
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SecondaryButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(50.dp).padding(top = 8.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TpTextPrimary), border = androidx.compose.foundation.BorderStroke(1.dp, TpBorderLight)) {
        Icon(icon, null, modifier = Modifier.size(19.dp)); Spacer(Modifier.width(8.dp)); Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun availableStorageLabel(): String {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            val stat = StatFs(context.filesDir.absolutePath)
            String.format(Locale.US, "%.1f GB", stat.availableBytes / (1024f * 1024f * 1024f))
        }.getOrDefault("-- GB")
    }
}
