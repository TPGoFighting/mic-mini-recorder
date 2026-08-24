package com.dji.recorder.ui.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dji.recorder.audio.StorageHelper
import com.dji.recorder.model.AppThemeStyle
import com.dji.recorder.model.AudioConfig
import com.dji.recorder.model.AudioFormatType
import com.dji.recorder.model.StorageLocationType
import com.dji.recorder.ui.floating.FloatingCapsuleManager
import com.dji.recorder.ui.theme.LocalThemeStyle
import com.dji.recorder.ui.theme.NeoAcidLime
import com.dji.recorder.ui.theme.NeoBadge
import com.dji.recorder.ui.theme.NeoBlack
import com.dji.recorder.ui.theme.NeoButton
import com.dji.recorder.ui.theme.NeoCyberYellow
import com.dji.recorder.ui.theme.NeoElectricCyan
import com.dji.recorder.ui.theme.NeoHotRed

/**
 * 音频编码、存储路径与视觉主题设置面板 (高对比度、防折行重构)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AudioSettingsDialog(
    currentConfig: AudioConfig,
    currentThemeStyle: AppThemeStyle,
    onOpenThemeSelection: () -> Unit,
    onSaveConfig: (AudioConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val borderColor = MaterialTheme.colorScheme.outline
    val globalStyle = LocalThemeStyle.current
    val isNeo = globalStyle == AppThemeStyle.NEO_BRUTALISM

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
            } catch (e: Exception) { /* ignore */ }

            val resolvedPath = StorageHelper.resolvePathFromTreeUri(context, uri) ?: uri.path ?: uri.toString()
            customFolderPath = resolvedPath
            customFolderUri = uri.toString()
            selectedStorage = StorageLocationType.CUSTOM_DIR
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp, top = 4.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 顶栏 Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isNeo) NeoCyberYellow else MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        if (isNeo) 2.5.dp else 1.dp,
                        borderColor,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isNeo) NeoBlack else MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = null,
                            tint = if (isNeo) NeoCyberYellow else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "AUDIO STUDIO CONFIG",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = if (isNeo) NeoBlack else MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isNeo) NeoBlack else MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = if (isNeo) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 0. 主题视觉风格选择入口卡片
            NeoSectionTitle(icon = Icons.Default.Palette, title = "THEME DESIGN STYLE • 视觉主题风格")
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                if (isNeo) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 3.dp, y = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(if (isNeo) 2.dp else 1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { onOpenThemeSelection() }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .border(1.5.dp, borderColor, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = currentThemeStyle.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "点此进入画廊切换 5 大设计风格",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NeoBadge(
                            text = currentThemeStyle.badge,
                            backgroundColor = NeoCyberYellow,
                            textColor = NeoBlack
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. 存储位置自定义
            NeoSectionTitle(icon = Icons.Default.Folder, title = "STORAGE DIRECTORY • 保存路径")
            Spacer(modifier = Modifier.height(8.dp))

            StorageLocationType.entries.forEach { loc ->
                val isSelected = selectedStorage == loc
                NeoStorageRow(
                    loc = loc,
                    isSelected = isSelected,
                    customPath = if (loc == StorageLocationType.CUSTOM_DIR) customFolderPath else null,
                    onSelect = {
                        if (loc == StorageLocationType.CUSTOM_DIR) {
                            folderPickerLauncher.launch(null)
                        } else {
                            selectedStorage = loc
                        }
                    },
                    onPickFolder = { folderPickerLauncher.launch(null) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. 悬浮胶囊控制
            NeoSectionTitle(icon = Icons.Default.GraphicEq, title = "FLUID CLOUD • 流体云悬浮胶囊")
            Spacer(modifier = Modifier.height(8.dp))

            var hasOverlayPermission by remember { mutableStateOf(FloatingCapsuleManager.hasOverlayPermission(context)) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (hasOverlayPermission) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant)
                    .border(if (isNeo) 2.dp else 1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .clickable {
                        if (!hasOverlayPermission) {
                            FloatingCapsuleManager.requestOverlayPermission(context)
                        }
                    }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = if (hasOverlayPermission) "悬浮胶囊已就绪 (支持锁屏置顶)" else "点此授权悬浮窗权限",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "录音时在屏幕及锁屏状态置顶显示微型秒表药丸",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                NeoBadge(
                    text = if (hasOverlayPermission) "ENABLED" else "GRANT",
                    backgroundColor = if (hasOverlayPermission) NeoAcidLime else NeoHotRed,
                    textColor = if (hasOverlayPermission) NeoBlack else Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. 输出格式
            NeoSectionTitle(icon = Icons.Default.Audiotrack, title = "AUDIO FORMAT • 输出格式")
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AudioFormatType.entries.forEach { format ->
                    val isSelected = selectedFormat == format
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .border(if (isNeo) 2.dp else 1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { selectedFormat = format }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = format.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. 采样率
            NeoSectionTitle(icon = Icons.Default.Equalizer, title = "SAMPLE RATE • 采样率")
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(48000, 44100, 32000, 16000).forEach { sr ->
                    val isSelected = selectedSampleRate == sr
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                            .border(if (isNeo) 2.dp else 1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { selectedSampleRate = sr }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${sr / 1000} kHz" + if (sr == 48000) " (推荐)" else "",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black),
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (selectedFormat != AudioFormatType.WAV) {
                Spacer(modifier = Modifier.height(16.dp))
                NeoSectionTitle(icon = Icons.Default.Speed, title = "BITRATE • 比特率")
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(320, 256, 192, 128).forEach { br ->
                        val isSelected = selectedBitrate == br
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeoElectricCyan else MaterialTheme.colorScheme.surfaceVariant)
                                .border(if (isNeo) 2.dp else 1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { selectedBitrate = br }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "$br kbps",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black),
                                color = if (isSelected) NeoBlack else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 保存按钮 (NeoButton)
            NeoButton(
                onClick = {
                    val newConfig = currentConfig.copy(
                        format = selectedFormat,
                        sampleRate = selectedSampleRate,
                        bitrateKbps = selectedBitrate,
                        isStereo = isStereo,
                        storageLocation = selectedStorage,
                        customFolderPath = customFolderPath,
                        customFolderUri = customFolderUri
                    )
                    onSaveConfig(newConfig)
                    onDismiss()
                },
                backgroundColor = MaterialTheme.colorScheme.primary,
                borderColor = borderColor
            ) {
                Text(
                    text = "SAVE & APPLY CONFIG",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun NeoSectionTitle(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun NeoStorageRow(
    loc: StorageLocationType,
    isSelected: Boolean,
    customPath: String?,
    onSelect: () -> Unit,
    onPickFolder: () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outline
    val globalStyle = LocalThemeStyle.current
    val isNeo = globalStyle == AppThemeStyle.NEO_BRUTALISM

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    if (isSelected) (if (isNeo) 2.dp else 1.5.dp) else 1.dp,
                    if (isSelected) (if (isNeo) borderColor else MaterialTheme.colorScheme.primary) else borderColor.copy(alpha = 0.35f),
                    RoundedCornerShape(8.dp)
                )
                .clickable { onSelect() }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = loc.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = if (loc == StorageLocationType.CUSTOM_DIR && !customPath.isNullOrBlank()) customPath else loc.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (loc == StorageLocationType.CUSTOM_DIR) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeoCyberYellow)
                            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                            .clickable { onPickFolder() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = NeoBlack,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "SELECT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = NeoBlack
                            )
                        }
                    }
                } else if (isSelected) {
                    NeoBadge(
                        text = "SELECTED",
                        backgroundColor = MaterialTheme.colorScheme.primary,
                        textColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
