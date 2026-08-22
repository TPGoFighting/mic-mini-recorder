package com.example.model

enum class RecordingState {
    IDLE,
    SEARCHING_BLUETOOTH,
    RECORDING,
    PAUSED,
    STOPPED,
    ERROR
}

enum class AudioFormatType(val extension: String, val displayName: String, val mimeType: String) {
    MP3("mp3", "MP3 常用", "audio/mpeg"),
    AAC_M4A("m4a", "AAC 高压缩", "audio/mp4a-latm"),
    WAV("wav", "PCM 无损", "audio/wav")
}

data class AudioDeviceItem(
    val id: Int,
    val name: String,
    val type: Int,
    val typeName: String,
    val isBluetooth: Boolean,
    val isDefault: Boolean = false
)

data class AudioRouteInfo(
    val deviceName: String = "未连接",
    val deviceType: Int = 0,
    val deviceId: Int = 0,
    val isExternalBluetooth: Boolean = false,
    val isFallback: Boolean = false,
    val description: String = "等待麦克风连接"
)

data class RecordingConfig(
    val strictBluetoothOnly: Boolean = true, // Safety invariant: never fall back to the phone mic
    val segmentDurationMinutes: Int = 30,
    val sampleRate: Int = 16_000,
    val bitrateKbps: Int = 128,
    val format: AudioFormatType = AudioFormatType.MP3,
    val preferredDeviceId: Int = 0, // 0 = Auto (Priority DJI Bluetooth)
    val isStereo: Boolean = false,
    val enableNoiseSuppression: Boolean = true,
    val keepScreenOn: Boolean = false
) {
    val segmentDurationMs: Long
        get() = if (segmentDurationMinutes <= 0) Long.MAX_VALUE else segmentDurationMinutes * 60 * 1000L
}

data class RecordingFileItem(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val lastModifiedMs: Long
) {
    val formattedDuration: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes % 60, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> String.format("%.2f MB", mb)
                kb >= 1.0 -> String.format("%.1f KB", kb)
                else -> "$sizeBytes B"
            }
        }
}
