package com.dji.recorder.audio

import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * 录音回放管理器
 */
class AudioPlayer {
    private val TAG = "AudioPlayer"
    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPlayingFile = MutableStateFlow<File?>(null)
    val currentPlayingFile: StateFlow<File?> = _currentPlayingFile.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun play(file: File) {
        if (!file.exists()) return

        stop()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    stop()
                }
                start()
            }
            _currentPlayingFile.value = file
            _isPlaying.value = true

            startProgressTracking()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio file: ${file.name}", e)
            stop()
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
            _currentPlayingFile.value = null
            _playbackProgress.value = 0f
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying && player.duration > 0) {
                        _playbackProgress.value = player.currentPosition.toFloat() / player.duration
                    }
                }
                delay(100)
            }
        }
    }
}
