package com.dji.recorder

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.dji.recorder.ui.DjiRecorderScreen
import com.dji.recorder.ui.DjiRecorderViewModel
import com.dji.recorder.ui.theme.DjiRecorderTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DjiRecorderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            DjiRecorderTheme(themeMode = themeMode) {
                DjiRecorderScreen(viewModel = viewModel)
            }
        }
    }

    /**
     * 捕获 DJI Mic / 蓝牙耳机物理按键（单击配对键 / 线控键 / 快门键 / 播放暂停键）
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_HEADSETHOOK,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE,
                KeyEvent.KEYCODE_MEDIA_RECORD,
                KeyEvent.KEYCODE_CAMERA -> {
                    Log.i("MainActivity", "Captured DJI Mic Physical Button Event: ${event.keyCode}")
                    viewModel.toggleRecording()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
