package com.dji.recorder.ui.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dji.recorder.audio.StorageHelper
import com.dji.recorder.model.AudioConfig
import com.dji.recorder.model.AudioFormatType
import com.dji.recorder.model.StorageLocationType
import com.dji.recorder.ui.floating.FloatingCapsuleManager
import com.dji.recorder.ui.theme.DjiGreen
import com.dji.recorder.ui.theme.DjiRed

/**
 * 音频编码与硬件参数设置模态面板 (含高度自定义存储路径选择器)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AudioSettingsDialog(
    currentConfig: AudioConfig,
    onSaveConfig: (AudioConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedFormat by remember { mutableStateOf(currentConfig.format) }
    var selectedSampleRate by remember { mutableIntStateOf(currentConfig.sampleRate) }
    var selectedBitrate by remember { mutableIntStateOf(currentConfig.bitrateKbps) }
    var isStereo by remember { mutableStateOf(currentConfig.isStereo) }
    var selectedStorage by remember { mutableStateOf(currentConfig.storageLocation) }
    var customFolderPath by remember { mutableStateOf(currentConfig.customFolderPath) }
    var customFolderUri by remember { mutableStateOf(currentConfig.customFolderUri) }

    // SAF 系统文件夹选择器
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if permission flags can't be persisted
            }
            val resolvedPath = StorageHelper.resolvePathFromTreeUri(context, uri)
            customFolderUri = uri.toString()
            customFolderPath = resolvedPath ?: uri.path
            selectedStorage = StorageLocationType.CUSTOM_DIR
        }
    }

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
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "专业录音与编码设置",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. 输出音频格式 (Format)
            SettingSectionTitle(title = "输出文件编码格式", icon = Icons.Default.Audiotrack)
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AudioFormatType.values().forEach { format ->
                    FormatOptionCard(
                        format = format,
                        isSelected = selectedFormat == format,
                        onSelect = { selectedFormat = format }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. 采样率 (Sample Rate)
            SettingSectionTitle(title = "硬件音频采样率", icon = Icons.Default.GraphicEq)
            Spacer(modifier = Modifier.height(10.dp))
            val sampleRates = listOf(16000, 32000, 44100, 48000, 96000)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                sampleRates.forEach { rate ->
                    ChipSelector(
                        text = if (rate == 48000) "48 kHz (官方推荐)" else "${rate / 1000} kHz",
                        isSelected = selectedSampleRate == rate,
                        onClick = { selectedSampleRate = rate }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. 比特率 (Bitrate)
            if (selectedFormat != AudioFormatType.WAV) {
                SettingSectionTitle(title = "压缩编码比特率", icon = Icons.Default.Speed)
                Spacer(modifier = Modifier.height(10.dp))
                val bitrates = listOf(128, 192, 256, 320)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    bitrates.forEach { br ->
                        ChipSelector(
                            text = if (br == 320) "$br kbps (最高品质)" else "$br kbps",
                            isSelected = selectedBitrate == br,
                            onClick = { selectedBitrate = br }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 4. 声道模式 (Channels)
            SettingSectionTitle(title = "声道模式", icon = Icons.Default.Audiotrack)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ChipSelector(
                    text = "单声道 (Mono • 推荐麦克风)",
                    isSelected = !isStereo,
                    modifier = Modifier.weight(1f),
                    onClick = { isStereo = false }
                )
                ChipSelector(
                    text = "双声道 (Stereo)",
                    isSelected = isStereo,
                    modifier = Modifier.weight(1f),
                    onClick = { isStereo = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. 存储存放位置 (Storage Location)
            SettingSectionTitle(title = "音频存储与输出目录 (高度自定义)", icon = Icons.Default.Folder)
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StorageLocationType.values().forEach { loc ->
                    val isSelectedLoc = selectedStorage == loc
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                BorderStroke(if (isSelectedLoc) 2.dp else 1.dp, if (isSelectedLoc) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                if (loc == StorageLocationType.CUSTOM_DIR) {
                                    selectedStorage = loc
                                    folderPickerLauncher.launch(null)
                                } else {
                                    selectedStorage = loc
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelectedLoc) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = loc.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (loc == StorageLocationType.CUSTOM_DIR && !customFolderPath.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "已绑定自选",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (loc == StorageLocationType.CUSTOM_DIR && !customFolderPath.isNullOrBlank()) {
                                        "当前路径: $customFolderPath (点击可更改)"
                                    } else {
                                        loc.description
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (loc == StorageLocationType.CUSTOM_DIR && isSelectedLoc) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelectedLoc) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 自研流体云悬浮胶囊状态与授权
            val hasOverlay = FloatingCapsuleManager.hasOverlayPermission(context)

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (hasOverlay) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, if (hasOverlay) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        FloatingCapsuleManager.requestOverlayPermission(context)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "自研流体云悬浮胶囊",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (hasOverlay) DjiGreen.copy(alpha = 0.2f) else DjiRed.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (hasOverlay) "已就绪" else "点此授权",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (hasOverlay) DjiGreen else DjiRed,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (hasOverlay) "录音时屏幕顶部全局悬浮显示走秒胶囊（支持锁屏置顶、拖拽与一键停止）" else "点击跳转开启「显示在其他应用上层」以激活顶部灵动胶囊",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 保存设置按钮
            Button(
                onClick = {
                    onSaveConfig(
                        AudioConfig(
                            format = selectedFormat,
                            sampleRate = selectedSampleRate,
                            bitrateKbps = selectedBitrate,
                            isStereo = isStereo,
                            storageLocation = selectedStorage,
                            customFolderPath = customFolderPath,
                            customFolderUri = customFolderUri
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "应用并保存配置",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun SettingSectionTitle(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun FormatOptionCard(
    format: AudioFormatType,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.primary
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) borderColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                RoundedCornerShape(16.dp)
            )
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = format.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = ".${format.extension}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = format.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChipSelector(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
