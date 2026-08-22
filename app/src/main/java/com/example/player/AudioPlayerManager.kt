package com.example.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentFilePath: String? = null,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L
)

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val progressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    _playerState.value = _playerState.value.copy(
                        isPlaying = true,
                        currentPositionMs = player.currentPosition.toLong(),
                        totalDurationMs = player.duration.toLong()
                    )
                    handler.postDelayed(this, 100)
                }
            }
        }
    }

    fun playFile(file: File) {
        if (!file.exists()) return

        if (_playerState.value.currentFilePath == file.absolutePath && mediaPlayer != null) {
            if (_playerState.value.isPlaying) {
                pause()
            } else {
                resume()
            }
            return
        }

        stop()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))
                prepare()
                start()
                setOnCompletionListener {
                    _playerState.value = _playerState.value.copy(
                        isPlaying = false,
                        currentPositionMs = 0L
                    )
                    handler.removeCallbacks(progressRunnable)
                }
            }

            _playerState.value = PlayerState(
                isPlaying = true,
                currentFilePath = file.absolutePath,
                currentPositionMs = 0L,
                totalDurationMs = mediaPlayer?.duration?.toLong() ?: 0L
            )
            handler.post(progressRunnable)
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to play audio file: ${file.name}", e)
            stop()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _playerState.value = _playerState.value.copy(isPlaying = false)
                handler.removeCallbacks(progressRunnable)
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            it.start()
            _playerState.value = _playerState.value.copy(isPlaying = true)
            handler.post(progressRunnable)
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
    }

    fun stop() {
        handler.removeCallbacks(progressRunnable)
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {}
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _playerState.value = PlayerState()
    }

    fun release() {
        stop()
    }
}
