package com.example.service

import com.example.model.AudioRouteInfo
import com.example.model.RecordingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RecordingServiceUiState(
    val state: RecordingState = RecordingState.IDLE,
    val routeInfo: AudioRouteInfo = AudioRouteInfo(),
    val elapsedDurationMs: Long = 0L,
    val segmentIndex: Int = 0,
    val currentSegmentFileName: String = "",
    val currentRmsDb: Float = -96f,
    val currentPeakAmp: Float = 0f,
    val recentWaveform: List<Float> = List(32) { 0.05f },
    val statusMessage: String = "就绪",
    val lastError: String? = null
)

object RecordingServiceBus {
    private val _uiState = MutableStateFlow(RecordingServiceUiState())
    val uiState: StateFlow<RecordingServiceUiState> = _uiState.asStateFlow()

    fun update(transform: (RecordingServiceUiState) -> RecordingServiceUiState) {
        _uiState.value = transform(_uiState.value)
    }

    fun reset() {
        _uiState.value = RecordingServiceUiState()
    }
}
