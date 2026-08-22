package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.audio.AudioUtils
import com.example.model.RecordingFileItem
import com.example.player.PlayerState
import com.example.ui.theme.TpBgLight
import com.example.ui.theme.TpBorderLight
import com.example.ui.theme.TpBorderSubtle
import com.example.ui.theme.TpCoral
import com.example.ui.theme.TpCoralSoft
import com.example.ui.theme.TpPurplePrimary
import com.example.ui.theme.TpPurpleSecondary
import com.example.ui.theme.TpPurpleSoft
import com.example.ui.theme.TpSurfaceElevated
import com.example.ui.theme.TpSurfaceLight
import com.example.ui.theme.TpTextMuted
import com.example.ui.theme.TpTextPrimary
import com.example.ui.theme.TpTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordingsList(
    recordings: List<RecordingFileItem>,
    playerState: PlayerState,
    onPlay: (RecordingFileItem) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSeek: (Long) -> Unit,
    onDelete: (RecordingFileItem) -> Unit,
    onRefresh: () -> Unit,
    onShare: (RecordingFileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var itemToDelete by remember { mutableStateOf<RecordingFileItem?>(null) }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("确认删除录音？", color = TpTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("删除后将无法恢复该分段录音文件：\n${itemToDelete?.name}", color = TpTextSecondary) },
            containerColor = TpSurfaceLight,
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToDelete?.let { onDelete(it) }
                        itemToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_btn")
                ) {
                    Text("删除", color = TpCoral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("取消", color = TpTextSecondary)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TpBgLight)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "录音媒体库",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TpTextPrimary
                )
                Text(
                    text = "共 ${recordings.size} 个分段录音文件",
                    fontSize = 11.sp,
                    color = TpTextMuted
                )
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(TpSurfaceLight)
                    .border(1.dp, TpBorderLight, CircleShape)
                    .testTag("refresh_recordings_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新录音列表",
                    tint = TpPurplePrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(TpPurpleSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = "暂无录音",
                            modifier = Modifier.size(32.dp),
                            tint = TpPurplePrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "暂无分段录音文件",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TpTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "录音完成后，分段音频将自动保存在此处",
                        fontSize = 12.sp,
                        color = TpTextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("recordings_lazy_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recordings, key = { it.path }) { item ->
                    val isCurrentPlaying = playerState.currentFilePath == item.path
                    val isPlayingNow = isCurrentPlaying && playerState.isPlaying

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(1.dp, RoundedCornerShape(20.dp), spotColor = Color(0x08000000))
                            .border(
                                1.dp,
                                if (isCurrentPlaying) TpPurplePrimary else TpBorderLight,
                                RoundedCornerShape(20.dp)
                            )
                            .testTag("recording_card_${item.name}"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = TpSurfaceLight
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Play / Pause circular button
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (isPlayingNow) TpCoralSoft else TpPurpleSoft)
                                        .clickable {
                                            if (isCurrentPlaying) {
                                                if (isPlayingNow) onPause() else onResume()
                                            } else {
                                                onPlay(item)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingNow) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlayingNow) "暂停" else "播放",
                                        tint = if (isPlayingNow) TpCoral else TpPurplePrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TpTextPrimary,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = dateFormatter.format(Date(item.lastModifiedMs)),
                                            fontSize = 11.sp,
                                            color = TpTextMuted
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = TpSurfaceElevated
                                        ) {
                                            Text(
                                                text = item.formattedSize,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TpPurplePrimary,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = { onShare(item) },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .testTag("btn_share_${item.name}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "分享录音",
                                            tint = TpTextSecondary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { itemToDelete = item },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .testTag("btn_delete_${item.name}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "删除录音",
                                            tint = TpCoral,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }

                            // Embedded Playback Seekbar
                            AnimatedVisibility(visible = isCurrentPlaying) {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                                    val currentPos = playerState.currentPositionMs.toFloat()
                                    val totalDur = playerState.totalDurationMs.toFloat().coerceAtLeast(1f)

                                    Slider(
                                        value = (currentPos / totalDur).coerceIn(0f, 1f),
                                        onValueChange = { ratio ->
                                            onSeek((ratio * totalDur).toLong())
                                        },
                                        modifier = Modifier.fillMaxWidth().height(24.dp),
                                        colors = SliderDefaults.colors(
                                            thumbColor = TpPurplePrimary,
                                            activeTrackColor = TpPurplePrimary,
                                            inactiveTrackColor = TpBorderLight
                                        )
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = AudioUtils.formatDuration(playerState.currentPositionMs),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TpPurplePrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = AudioUtils.formatDuration(playerState.totalDurationMs),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TpTextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
