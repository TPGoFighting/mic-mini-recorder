package com.example.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioRouteController
import com.example.data.RecordingsRepository
import com.example.model.AudioDeviceItem
import com.example.model.AudioFormatType
import com.example.model.AudioRouteInfo
import com.example.model.RecordingConfig
import com.example.model.RecordingFileItem
import com.example.model.RecordingState
import com.example.player.AudioPlayerManager
import com.example.player.PlayerState
import com.example.service.RecordingService
import com.example.service.RecordingServiceBus
import com.example.service.RecordingServiceUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecordingsRepository(application)
    private val playerManager = AudioPlayerManager(application)
    private val routeController = AudioRouteController(application)

    val serviceState: StateFlow<RecordingServiceUiState> = RecordingServiceBus.uiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecordingServiceUiState())

    val playerState: StateFlow<PlayerState> = playerManager.playerState

    private val preferences = application.getSharedPreferences("tp_recorder_preferences", 0)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<RecordingConfig> = _config.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<AudioDeviceItem>>(emptyList())
    val availableDevices: StateFlow<List<AudioDeviceItem>> = _availableDevices.asStateFlow()

    private val _recordings = MutableStateFlow<List<RecordingFileItem>>(emptyList())
    val recordings: StateFlow<List<RecordingFileItem>> = _recordings.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _showSettingsSheet = MutableStateFlow(false)
    val showSettingsSheet: StateFlow<Boolean> = _showSettingsSheet.asStateFlow()

    private val _idleRouteInfo = MutableStateFlow(AudioRouteInfo())
    val idleRouteInfo: StateFlow<AudioRouteInfo> = _idleRouteInfo.asStateFlow()

    init {
        routeController.onDevicesChangedListener = {
            refreshRouteInfo()
        }
        refreshRecordings()
        refreshRouteInfo()
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
        if (index == 1) {
            refreshRecordings()
        }
    }

    fun setShowSettings(show: Boolean) {
        _showSettingsSheet.value = show
        if (show) {
            refreshRouteInfo()
        }
    }

    fun updateConfig(newConfig: RecordingConfig) {
        val safeConfig = newConfig.copy(strictBluetoothOnly = true)
        _config.value = safeConfig
        persistConfig(safeConfig)
        refreshRouteInfo()
    }

    fun selectPreferredDevice(deviceId: Int) {
        updateConfig(_config.value.copy(preferredDeviceId = deviceId))
    }

    fun refreshRouteInfo() {
        val prefId = _config.value.preferredDeviceId
        _idleRouteInfo.value = routeController.getCurrentRouteInfo(preferredDeviceId = prefId)
        _availableDevices.value = routeController.getAvailableInputDevices()
    }

    fun refreshRecordings() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.fetchRecordings()
            _recordings.value = list
        }
    }

    fun startRecording() {
        val currentConfig = _config.value
        val intent = Intent(getApplication(), RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra(RecordingService.EXTRA_STRICT_BT, currentConfig.strictBluetoothOnly)
            putExtra(RecordingService.EXTRA_SEGMENT_MINUTES, currentConfig.segmentDurationMinutes)
            putExtra(RecordingService.EXTRA_SAMPLE_RATE, currentConfig.sampleRate)
            putExtra(RecordingService.EXTRA_BITRATE_KBPS, currentConfig.bitrateKbps)
            putExtra(RecordingService.EXTRA_FORMAT, currentConfig.format.name)
            putExtra(RecordingService.EXTRA_PREFERRED_DEVICE_ID, currentConfig.preferredDeviceId)
        }
        ContextCompat.startForegroundService(getApplication(), intent)
    }

    fun stopRecording() {
        val intent = Intent(getApplication(), RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        ContextCompat.startForegroundService(getApplication(), intent)
        viewModelScope.launch {
            delay(800L)
            refreshRecordings()
        }
    }

    fun pauseRecording() {
        val intent = Intent(getApplication(), RecordingService::class.java).apply {
            action = RecordingService.ACTION_PAUSE
        }
        ContextCompat.startForegroundService(getApplication(), intent)
    }

    fun resumeRecording() {
        val intent = Intent(getApplication(), RecordingService::class.java).apply {
            action = RecordingService.ACTION_RESUME
        }
        ContextCompat.startForegroundService(getApplication(), intent)
    }

    fun playRecording(item: RecordingFileItem) {
        playerManager.playFile(File(item.path))
    }

    fun pausePlayer() {
        playerManager.pause()
    }

    fun resumePlayer() {
        playerManager.resume()
    }

    fun seekPlayer(posMs: Long) {
        playerManager.seekTo(posMs)
    }

    fun stopPlayer() {
        playerManager.stop()
    }

    fun deleteRecording(item: RecordingFileItem) {
        viewModelScope.launch(Dispatchers.IO) {
            if (playerState.value.currentFilePath == item.path) {
                playerManager.stop()
            }
            repository.deleteRecording(item.path)
            refreshRecordings()
        }
    }

    fun getShareIntent(item: RecordingFileItem): Intent? {
        return repository.createShareIntent(item.path)
    }

    override fun onCleared() {
        playerManager.release()
        routeController.release()
        super.onCleared()
    }

    private fun loadConfig(): RecordingConfig = RecordingConfig(
        strictBluetoothOnly = true,
        segmentDurationMinutes = preferences.getInt("segment_minutes", 30),
        sampleRate = preferences.getInt("sample_rate", 16_000),
        bitrateKbps = preferences.getInt("bitrate_kbps", 128),
        format = runCatching {
            AudioFormatType.valueOf(preferences.getString("format", AudioFormatType.MP3.name)!!)
        }.getOrDefault(AudioFormatType.MP3),
        preferredDeviceId = preferences.getInt("preferred_device_id", 0),
        isStereo = preferences.getBoolean("stereo", false),
        enableNoiseSuppression = preferences.getBoolean("noise_suppression", true),
        keepScreenOn = preferences.getBoolean("keep_screen_on", false)
    )

    private fun persistConfig(config: RecordingConfig) {
        preferences.edit()
            .putInt("segment_minutes", config.segmentDurationMinutes)
            .putInt("sample_rate", config.sampleRate)
            .putInt("bitrate_kbps", config.bitrateKbps)
            .putString("format", config.format.name)
            .putInt("preferred_device_id", config.preferredDeviceId)
            .putBoolean("stereo", config.isStereo)
            .putBoolean("noise_suppression", config.enableNoiseSuppression)
            .putBoolean("keep_screen_on", config.keepScreenOn)
            .apply()
    }
}
