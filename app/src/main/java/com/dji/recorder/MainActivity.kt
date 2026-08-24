package com.dji.recorder

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.dji.recorder.bluetooth.DjiHardwareButtonManager
import com.dji.recorder.model.RecorderStatus
import com.dji.recorder.ui.DjiRecorderScreen
import com.dji.recorder.ui.DjiRecorderViewModel
import com.dji.recorder.ui.theme.DjiRecorderTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DjiRecorderViewModel by viewModels()
    private var mediaSession: MediaSession? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var djiButtonManager: DjiHardwareButtonManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        setupMediaSession()

        // 启动大疆专用底层协议按键监听器
        djiButtonManager = DjiHardwareButtonManager(this) {
            handleHardwareTrigger("大疆专属底层通道")
        }
        djiButtonManager?.startListening()

        setContent {
            val themeStyle by viewModel.themeStyle.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()
            DjiRecorderTheme(themeStyle = themeStyle, themeMode = themeMode) {
                DjiRecorderScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requestSystemAudioFocus()
        mediaSession?.isActive = true
        djiButtonManager?.startListening()
    }

    private fun setupMediaSession() {
        try {
            mediaSession = MediaSession(this, "DjiMicMiniSession").apply {
                setCallback(object : MediaSession.Callback() {
                    override fun onMediaButtonEvent(mediaButtonIntent: android.content.Intent): Boolean {
                        val keyEvent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            mediaButtonIntent.getParcelableExtra(android.content.Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            mediaButtonIntent.getParcelableExtra(android.content.Intent.EXTRA_KEY_EVENT)
                        }
                        if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                            Log.i("MainActivity", "🎯 MediaSession onMediaButtonEvent: ${keyEvent.keyCode}")
                            handleHardwareTrigger("MediaSession [Key=${keyEvent.keyCode}]")
                            return true
                        }
                        return super.onMediaButtonEvent(mediaButtonIntent)
                    }

                    override fun onPlay() {
                        handleHardwareTrigger("MediaSession:onPlay")
                    }

                    override fun onPause() {
                        handleHardwareTrigger("MediaSession:onPause")
                    }

                    override fun onStop() {
                        viewModel.stopRecording()
                    }
                })

                val state = PlaybackState.Builder()
                    .setActions(
                        PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_STOP
                    )
                    .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                    .build()
                setPlaybackState(state)
                isActive = true
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to setup MediaSession", e)
        }
    }

    private fun requestSystemAudioFocus() {
        try {
            audioManager?.let { am ->
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener { /* focus change */ }
                    .build()
                am.requestAudioFocus(audioFocusRequest!!)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to request audio focus", e)
        }
    }

    private fun handleHardwareTrigger(source: String) {
        val currentStatus = viewModel.status.value
        Log.i("MainActivity", ">>> HARDWARE BUTTON TRIGGERED BY [$source], current status: $currentStatus <<<")

        if (currentStatus == RecorderStatus.RECORDING) {
            Toast.makeText(this, "⏹️ 收到大疆麦克风按键：录音已停止并保存", Toast.LENGTH_SHORT).show()
            viewModel.stopRecording()
        } else {
            Toast.makeText(this, "🎙️ 收到大疆麦克风按键：开始录音！", Toast.LENGTH_SHORT).show()
            viewModel.startRecording()
        }
    }

    /**
     * 拦截所有输入事件，精准过滤手机自身物理按键，放行并触发所有大疆蓝牙/耳机线控按键
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val dev = event.device
        val devName = dev?.name ?: "null"
        Log.i("MainActivity", "🔥 RAW dispatchKeyEvent: keyCode=${event.keyCode}, action=${event.action}, device='$devName', isExternal=${dev?.isExternal}")

        if (event.action == KeyEvent.ACTION_DOWN) {
            val isPhoneInternal = devName.contains("gpio-keys", ignoreCase = true) ||
                                  devName.contains("pmic", ignoreCase = true)

            // 如果不是手机自带按键（例如来自大疆麦克风、蓝牙设备、虚拟耳机）
            if (!isPhoneInternal) {
                Log.i("MainActivity", "🎯 External Key Captured in dispatchKeyEvent: keyCode=${event.keyCode}")
                handleHardwareTrigger("External Device Key [${event.keyCode}]")
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val devName = event?.device?.name ?: "null"
        Log.i("MainActivity", "🔥 RAW onKeyDown: keyCode=$keyCode, device='$devName'")
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        djiButtonManager?.stopListening()
        try {
            mediaSession?.release()
            mediaSession = null
        } catch (e: Exception) { /* ignore */ }
    }
}
