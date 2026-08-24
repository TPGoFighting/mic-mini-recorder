package com.dji.recorder.model

import android.media.AudioDeviceInfo
import java.io.File

/**
 * 录音器核心状态机
 */
enum class RecorderStatus {
    NO_DEVICE,      // 未检测到蓝牙麦克风
    READY,          // 蓝牙麦克风已就绪
    CONNECTING_SCO, // 正在建立蓝牙 SCO 音频链路
    RECORDING,      // 正在录音
    PROCESSING_AI,  // 正在执行深度降噪后处理
    ENCODING,       // 正在压缩转码 (MP3/AAC)
    SAVED,          // 文件已保存
    ERROR           // 发生异常
}

/**
 * 音频存放存储位置类型
 */
enum class StorageLocationType(
    val title: String,
    val description: String
) {
    PUBLIC_RECORDINGS("系统录音目录", "/Recordings/DJIRecorder (系统文件管理与电脑直接可见)"),
    PUBLIC_MUSIC("系统音乐目录", "/Music/DJIRecorder (音乐播放器可直接扫描)"),
    PUBLIC_DOWNLOAD("公共下载目录", "/Download/DJIRecorder (方便第三方应用快速访问与发送)"),
    APP_INTERNAL("应用私有沙盒", "Android/data/com.dji.recorder (独立安全私密存储)"),
    CUSTOM_DIR("自定义存储路径", "用户自选文件夹 (支持系统任意目录与SD卡)")
}

/**
 * 支持的音频编码格式
 */
enum class AudioFormatType(
    val title: String,
    val extension: String,
    val description: String
) {
    WAV("WAV 无损", "wav", "未压缩 PCM 原始无损音质，适合后期专业剪辑"),
    MP3("MP3 格式", "mp3", "通用性极强的高压缩音频，兼容绝大多数播放设备"),
    AAC("AAC / M4A", "m4a", "广播级高保真压缩，相同体积下音质优于 MP3")
}

/**
 * 降噪模式定义
 */
enum class NoiseReductionMode(
    val title: String,
    val badge: String,
    val subtitle: String,
    val description: String
) {
    OFF(
        title = "关闭降噪 (推荐)",
        badge = "原声",
        subtitle = "无损原声直通",
        description = "直通 DJI 麦克风原始高清无损音频流，不施加软件滤波，配合机身黄灯降噪效果最佳。"
    ),
    AI_HIGH(
        title = "演播室专业降噪",
        badge = "演播室",
        subtitle = "Audacity / WebRTC 工业落地标准",
        description = "采用 Audacity / WebRTC 工业落地级平滑降噪架构：80Hz 低切 + 跨频点连续平滑 + 15ms 动态时间包络，100% 保持自然人声温度，杜绝怪异电音与机械声。"
    ),
    FAST_LOW(
        title = "系统快速降噪",
        badge = "快速",
        subtitle = "Android 实时硬件滤波",
        description = "录音时使用 Android 系统的基础实时噪声抑制与增益均衡处理。"
    )
}

/**
 * 音频全局配置数据类
 */
data class AudioConfig(
    val format: AudioFormatType = AudioFormatType.MP3,
    val sampleRate: Int = 48000,
    val bitrateKbps: Int = 320,
    val isStereo: Boolean = false,
    val storageLocation: StorageLocationType = StorageLocationType.PUBLIC_RECORDINGS,
    val customFolderPath: String? = null,
    val customFolderUri: String? = null
)

/**
 * 主题模式
 */
enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * 蓝牙音频设备元数据
 */
data class BluetoothMicDevice(
    val id: Int,
    val name: String,
    val address: String?,
    val type: Int,
    val rawDeviceInfo: AudioDeviceInfo
)

/**
 * 录音历史列表项
 */
data class RecordingItem(
    val file: File,
    val fileName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val createdAt: Long,
    val sampleRate: Int,
    val formatType: AudioFormatType,
    val denoiseMode: NoiseReductionMode
)
