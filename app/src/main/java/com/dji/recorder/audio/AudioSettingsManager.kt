package com.dji.recorder.audio

import android.content.Context
import android.content.SharedPreferences
import com.dji.recorder.model.AudioConfig
import com.dji.recorder.model.AudioFormatType
import com.dji.recorder.model.StorageLocationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 音频配置持久化管理器
 */
class AudioSettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dji_audio_settings", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<AudioConfig> = _config.asStateFlow()

    private fun loadConfig(): AudioConfig {
        val formatName = prefs.getString("format", AudioFormatType.MP3.name) ?: AudioFormatType.MP3.name
        val format = try {
            AudioFormatType.valueOf(formatName)
        } catch (e: Exception) {
            AudioFormatType.MP3
        }

        val storageName = prefs.getString("storage_location", StorageLocationType.PUBLIC_RECORDINGS.name)
            ?: StorageLocationType.PUBLIC_RECORDINGS.name
        val storageLocation = try {
            StorageLocationType.valueOf(storageName)
        } catch (e: Exception) {
            StorageLocationType.PUBLIC_RECORDINGS
        }

        val sampleRate = prefs.getInt("sample_rate", 48000)
        val bitrate = prefs.getInt("bitrate_kbps", 320)
        val isStereo = prefs.getBoolean("is_stereo", false)
        val customPath = prefs.getString("custom_folder_path", null)
        val customUri = prefs.getString("custom_folder_uri", null)

        return AudioConfig(
            format = format,
            sampleRate = sampleRate,
            bitrateKbps = bitrate,
            isStereo = isStereo,
            storageLocation = storageLocation,
            customFolderPath = customPath,
            customFolderUri = customUri
        )
    }

    fun updateConfig(newConfig: AudioConfig) {
        prefs.edit()
            .putString("format", newConfig.format.name)
            .putString("storage_location", newConfig.storageLocation.name)
            .putInt("sample_rate", newConfig.sampleRate)
            .putInt("bitrate_kbps", newConfig.bitrateKbps)
            .putBoolean("is_stereo", newConfig.isStereo)
            .putString("custom_folder_path", newConfig.customFolderPath)
            .putString("custom_folder_uri", newConfig.customFolderUri)
            .apply()

        _config.value = newConfig
    }
}
