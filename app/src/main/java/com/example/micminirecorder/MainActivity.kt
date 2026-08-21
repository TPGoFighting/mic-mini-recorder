package com.example.micminirecorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        status = TextView(this).apply {
            textSize = 16f
            text = "请先连接 DJI Mic Mini 发射器，然后点击开始。"
            setPadding(32, 32, 32, 24)
        }
        startButton = Button(this).apply {
            text = "开始录音"
            setOnClickListener { startRecording() }
        }
        val stopButton = Button(this).apply {
            text = "停止录音"
            setOnClickListener { stopRecording() }
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(status)
            addView(startButton)
            addView(stopButton)
        })

        requestRequiredPermissions()
    }

    private fun requestRequiredPermissions() {
        val permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQUEST_PERMISSIONS)
        }
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            status.text = "未获得麦克风权限。"
            requestRequiredPermissions()
            return
        }

        val intent = Intent(this, RecordingService::class.java)
            .setAction(RecordingService.ACTION_START)
        ContextCompat.startForegroundService(this, intent)
        status.text = "录音服务已启动。请查看常驻通知确认实际使用的是 DJI 蓝牙麦克风。"
    }

    private fun stopRecording() {
        startService(Intent(this, RecordingService::class.java).setAction(RecordingService.ACTION_STOP))
        status.text = "正在停止录音。"
    }

    companion object {
        private const val REQUEST_PERMISSIONS = 1001
    }
}
