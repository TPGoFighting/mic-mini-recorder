package com.dji.recorder.ui

import android.app.Application
import android.content.Intent
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.util.Log
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dji.recorder.audio.AudioPlayer
import com.dji.recorder.audio.AudioPostProcessor
import com.dji.recorder.audio.AudioSettingsManager
import com.dji.recorder.audio.BluetoothMicManager
import com.dji.recorder.audio.MediaCodecAudioEncoder
import com.dji.recorder.audio.StorageHelper
import com.dji.recorder.audio.WavAudioRecorder
import com.dji.recorder.model.AppThemeMode
import com.dji.recorder.model.AudioConfig
import com.dji.recorder.model.AudioFormatType
import com.dji.recorder.model.BluetoothMicDevice
import com.dji.recorder.model.NoiseReductionMode
import com.dji.recorder.model.RecorderStatus
import com.dji.recorder.model.RecordingItem
import com.dji.recorder.service.DjiRecordingService
import com.dji.recorder.ui.floating.FloatingCapsuleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DjiRecorderViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "DjiRecorderViewModel"
    private val micManager = BluetoothMicManager(application)
    private val recorder = WavAudioRecorder(application)
    private val postProcessor = AudioPostProcessor()
    private val encoder = MediaCodecAudioEncoder()
    private val settingsManager = AudioSettingsManager(application)
    private val player = AudioPlayer()
    private var mediaSession: MediaSession? = null

    private val _status = MutableStateFlow(RecorderStatus.NO_DEVICE)
    val status: StateFlow<RecorderStatus> = _status.asStateFlow()

    val connectedMic: StateFlow<BluetoothMicDevice?> = micManager.connectedDevice
    val currentDecibels: StateFlow<Float> = recorder.currentDecibels
    val durationMs: StateFlow<Long> = recorder.recordingDurationMs
    val activeRoutedDevice: StateFlow<String> = recorder.activeRoutedDevice

    private val _amplitudeHistory = MutableStateFlow<List<Float>>(emptyList())
    val amplitudeHistory: StateFlow<List<Float>> = _amplitudeHistory.asStateFlow()

    private val _recordingsList = MutableStateFlow<List<RecordingItem>>(emptyList())
    val recordingsList: StateFlow<List<RecordingItem>> = _recordingsList.asStateFlow()

    val isPlaying: StateFlow<Boolean> = player.isPlaying
    val currentPlayingFile: StateFlow<File?> = player.currentPlayingFile
    val playbackProgress: StateFlow<Float> = player.playbackProgress

    // 降噪模式（默认进入时为关闭原声直通）
    private val _noiseReductionMode = MutableStateFlow(NoiseReductionMode.OFF)
    val noiseReductionMode: StateFlow<NoiseReductionMode> = _noiseReductionMode.asStateFlow()

    // 全局音频设置（格式/采样率/比特率/声道）
    val audioConfig: StateFlow<AudioConfig> = settingsManager.config

    // 主题切换（跟随系统 / 深色 / 浅色）
    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    // 编码/AI 处理进度
    private val _processingProgress = MutableStateFlow(0f)
    val processingProgress: StateFlow<Float> = _processingProgress.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        micManager.startListening(
            onDisconnected = {
                onBluetoothMicDisconnected()
            }
        )

        viewModelScope.launch {
            micManager.connectedDevice.collect { device ->
                if (device != null) {
                    if (_status.value == RecorderStatus.NO_DEVICE || _status.value == RecorderStatus.ERROR) {
                        _status.value = RecorderStatus.READY
                    }
                } else {
                    if (_status.value == RecorderStatus.RECORDING) {
                        onBluetoothMicDisconnected()
                    } else {
                        _status.value = RecorderStatus.NO_DEVICE
                    }
                }
            }
        }

        viewModelScope.launch {
            recorder.currentAmplitude.collect { amp ->
                if (_status.value == RecorderStatus.RECORDING) {
                    _amplitudeHistory.value = (_amplitudeHistory.value + amp).takeLast(60)
                }
            }
        }

        refreshRecordingsList()
        setupMediaSession()
    }

    fun setNoiseReductionMode(mode: NoiseReductionMode) {
        if (_status.value != RecorderStatus.RECORDING) {
            _noiseReductionMode.value = mode
            _toastMessage.value = "降噪模式: ${mode.title} (${mode.badge})"
        } else {
            _toastMessage.value = "录音中无法切换降噪模式"
        }
    }

    fun updateAudioConfig(newConfig: AudioConfig) {
        if (_status.value != RecorderStatus.RECORDING) {
            settingsManager.updateConfig(newConfig)
            _toastMessage.value = "已更新参数: ${newConfig.format.title} • ${newConfig.sampleRate / 1000}kHz • ${newConfig.bitrateKbps}kbps"
        } else {
            _toastMessage.value = "录音中无法修改音频参数"
        }
    }

    fun toggleTheme() {
        _themeMode.value = when (_themeMode.value) {
            AppThemeMode.SYSTEM -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.LIGHT
            AppThemeMode.LIGHT -> AppThemeMode.SYSTEM
        }
        val themeName = when (_themeMode.value) {
            AppThemeMode.SYSTEM -> "跟随系统"
            AppThemeMode.DARK -> "深色模式"
            AppThemeMode.LIGHT -> "浅色模式"
        }
        _toastMessage.value = "已切换为: $themeName"
    }

    fun refreshRecordingsList() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val targetDir = StorageHelper.getTargetDirectory(app, audioConfig.value)
            val internalDir = File(app.filesDir, "recordings")

            val directoriesToScan = setOf(targetDir, internalDir)
            val allFiles = directoriesToScan.flatMap { dir ->
                if (dir.exists()) {
                    dir.listFiles { f ->
                        f.extension.equals("wav", ignoreCase = true) ||
                        f.extension.equals("mp3", ignoreCase = true) ||
                        f.extension.equals("m4a", ignoreCase = true)
                    }?.toList() ?: emptyList()
                } else emptyList()
            }.distinctBy { it.absolutePath }.sortedByDescending { it.lastModified() }

            val config = audioConfig.value
            val items = allFiles.map { f ->
                val formatType = when (f.extension.lowercase()) {
                    "mp3" -> AudioFormatType.MP3
                    "m4a" -> AudioFormatType.AAC
                    else -> AudioFormatType.WAV
                }
                val audioBytes = (f.length() - 44).coerceAtLeast(0)
                val duration = (audioBytes * 1000L) / (config.sampleRate * 2)
                val denoise = when {
                    f.name.contains("dpdfnet2", ignoreCase = true) -> NoiseReductionMode.AI_HIGH
                    else -> NoiseReductionMode.FAST_LOW
                }
                RecordingItem(
                    file = f,
                    fileName = f.name,
                    durationMs = duration,
                    sizeBytes = f.length(),
                    createdAt = f.lastModified(),
                    sampleRate = config.sampleRate,
                    formatType = formatType,
                    denoiseMode = denoise
                )
            }
            _recordingsList.value = items
        }
    }

    fun scanForBluetoothMic() {
        val dev = micManager.refreshConnectedBluetoothMics()
        if (dev == null) {
            _status.value = RecorderStatus.NO_DEVICE
            _toastMessage.value = "未检测到蓝牙麦克风，请在系统设置中配对"
        } else {
            _status.value = RecorderStatus.READY
            _toastMessage.value = "已就绪: ${dev.name}"
        }
    }

    fun toggleRecording() {
        if (_status.value == RecorderStatus.RECORDING) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    fun startRecording() {
        val mic = connectedMic.value
        if (mic == null) {
            _status.value = RecorderStatus.NO_DEVICE
            _toastMessage.value = "无法启动：未连接蓝牙麦克风"
            return
        }

        viewModelScope.launch {
            micManager.activateMicRouting(mic)
            if (mic.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                var waited = 0
                while (!micManager.isScoConnected.value && waited < 1000) {
                    kotlinx.coroutines.delay(100)
                    waited += 100
                }
            }
            _amplitudeHistory.value = emptyList()
            val result = recorder.startRecording(
                targetMic = mic,
                mode = _noiseReductionMode.value,
                config = audioConfig.value
            )
            if (result.isSuccess) {
                _status.value = RecorderStatus.RECORDING
                DjiRecordingService.startService(getApplication())
                FloatingCapsuleManager.show(getApplication()) {
                    stopRecording()
                }
            } else {
                micManager.deactivateMicRouting()
                _status.value = RecorderStatus.ERROR
                _toastMessage.value = "启动录音失败: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun stopRecording() {
        if (_status.value != RecorderStatus.RECORDING) return

        val currentMode = _noiseReductionMode.value
        val config = audioConfig.value
        _status.value = RecorderStatus.SAVED
        DjiRecordingService.stopService(getApplication())
        FloatingCapsuleManager.hide()

        viewModelScope.launch {
            val activeRaw = withContext(Dispatchers.IO) {
                recorder.stopRecording()
            }
            micManager.deactivateMicRouting()
            var activeFile = activeRaw

            if (activeFile != null) {
                // 1. 演播室级专业降噪后处理 (Audacity / WebRTC APM 工业标准)
                if (currentMode == NoiseReductionMode.AI_HIGH) {
                    _status.value = RecorderStatus.PROCESSING_AI
                    _toastMessage.value = "正在使用演播室级算法优化人声..."
                    val denoisedWav = postProcessor.processDpdfNet2Hr(activeFile) { progress ->
                        _processingProgress.value = progress
                    }
                    activeFile = denoisedWav
                }

                // 2. 格式转码 (MP3 / AAC)
                if (config.format != AudioFormatType.WAV) {
                    _status.value = RecorderStatus.ENCODING
                    _toastMessage.value = "正在转码为 ${config.format.title} (${config.bitrateKbps}kbps)..."
                    val encoded = encoder.encodePcmFile(activeFile, config) { progress ->
                        _processingProgress.value = progress
                    }
                    if (activeFile.absolutePath != encoded.absolutePath && activeFile.exists()) {
                        activeFile.delete()
                    }
                    _toastMessage.value = "已保存: ${encoded.name}"
                } else {
                    _toastMessage.value = "已保存无损 WAV: ${activeFile.name}"
                }

                refreshRecordingsList()
            }
            _status.value = if (connectedMic.value != null) RecorderStatus.READY else RecorderStatus.NO_DEVICE
        }
    }

    private fun onBluetoothMicDisconnected() {
        Log.w(TAG, "Bluetooth mic disconnected!")
        if (_status.value == RecorderStatus.RECORDING) {
            recorder.abortRecording()
            micManager.deactivateMicRouting()
            DjiRecordingService.stopService(getApplication())
            FloatingCapsuleManager.hide()
            _status.value = RecorderStatus.ERROR
            _toastMessage.value = "⚠️ 蓝牙麦克风已断开！录音已紧急中止并保存已录部分。"
            refreshRecordingsList()
        } else {
            _status.value = RecorderStatus.NO_DEVICE
        }
    }

    fun playRecording(item: RecordingItem) {
        player.play(item.file)
    }

    fun stopPlayback() {
        player.stop()
    }

    fun deleteRecording(item: RecordingItem) {
        if (player.currentPlayingFile.value?.absolutePath == item.file.absolutePath) {
            player.stop()
        }
        item.file.delete()
        refreshRecordingsList()
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    private fun setupMediaSession() {
        try {
            mediaSession = MediaSession(getApplication(), "DjiMicButtonHandler").apply {
                setCallback(object : MediaSession.Callback() {
                    override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                        val keyEvent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                        }
                        if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                            Log.i(TAG, "DJI Mic Link MediaButtonEvent received: ${keyEvent.keyCode}")
                            when (keyEvent.keyCode) {
                                KeyEvent.KEYCODE_HEADSETHOOK,
                                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                                KeyEvent.KEYCODE_MEDIA_PLAY,
                                KeyEvent.KEYCODE_MEDIA_PAUSE,
                                KeyEvent.KEYCODE_MEDIA_RECORD,
                                KeyEvent.KEYCODE_CAMERA -> {
                                    toggleRecording()
                                    return true
                                }
                            }
                        }
                        return super.onMediaButtonEvent(mediaButtonIntent)
                    }

                    override fun onPlay() { toggleRecording() }
                    override fun onPause() { toggleRecording() }
                    override fun onStop() { if (_status.value == RecorderStatus.RECORDING) toggleRecording() }
                })

                setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
                setPlaybackState(
                    PlaybackState.Builder()
                        .setActions(
                            PlaybackState.ACTION_PLAY or
                            PlaybackState.ACTION_PAUSE or
                            PlaybackState.ACTION_PLAY_PAUSE or
                            PlaybackState.ACTION_STOP
                        )
                        .setState(PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                        .build()
                )
                isActive = true
            }
            Log.i(TAG, "MediaSession created for DJI Mic physical button listener")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to setup MediaSession", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            mediaSession?.isActive = false
            mediaSession?.release()
            mediaSession = null
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing MediaSession", e)
        }
        micManager.stopListening()
        recorder.abortRecording()
        player.stop()
    }
}
