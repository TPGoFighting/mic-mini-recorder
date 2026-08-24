package com.dji.recorder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dji.recorder.MainActivity
import com.dji.recorder.R

/**
 * 专为 ColorOS 15/16 流体云 (Fluid Cloud / 状态栏胶囊) 与长时间后台保活打造的前台服务。
 * 核心技术规范：
 * 1. 适配 ColorOS 14/15/16 泛在服务与 Android 14+ Live Updates 实时活动规范。
 * 2. 注入 Pantanal / Oplus 流体云胶囊元数据 (oplus.fluid_cloud, capsule type, CATEGORY_STOPWATCH, chronometer)。
 * 3. 持有 PARTIAL_WAKE_LOCK 电源锁，阻断系统进程清理，保障 10 小时后台不掉线。
 */
class DjiRecordingService : Service() {

    companion object {
        private const val TAG = "DjiRecordingService"
        const val CHANNEL_ID = "dji_fluid_cloud_live_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START_RECORDING = "com.dji.recorder.action.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.dji.recorder.action.STOP_RECORDING"

        fun startService(context: Context) {
            val intent = Intent(context, DjiRecordingService::class.java).apply {
                action = ACTION_START_RECORDING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, DjiRecordingService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            context.startService(intent)
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var recordingStartTime: Long = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                recordingStartTime = System.currentTimeMillis()
                val notification = buildFluidCloudNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                Log.i(TAG, "DjiRecordingService running with ColorOS Fluid Cloud Notification")
            }
            ACTION_STOP_RECORDING -> {
                Log.i(TAG, "DjiRecordingService stopping...")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        Log.i(TAG, "DjiRecordingService destroyed")
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "DjiRecorder::RecordingWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // 支持最长 12 小时灭屏稳定录音
            }
            Log.i(TAG, "WakeLock acquired successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.i(TAG, "WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DJI 实时录音流体云",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "ColorOS 流体云状态栏胶囊与实时录音计时"
                setShowBadge(true)
                enableLights(true)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * 构建适配 ColorOS 14/15/16 泛在流体云与 Android 14+ Live Updates 的胶囊通知
     */
    private fun buildFluidCloudNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, DjiRecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = try {
            BitmapFactory.decodeResource(resources, R.drawable.app_logo)
        } catch (e: Exception) { null }

        // ColorOS 15/16 流体云 (Pantanal Framework) 核心扩展参数
        val fluidCloudExtras = Bundle().apply {
            putBoolean("android.support.actionShowsUserInterface", true)
            putBoolean("oplus.fluid_cloud.enabled", true)
            putBoolean("com.oplus.fluid_cloud", true)
            putString("oplus.notification.category", "live_notification")
            putString("oplus_view_type", "capsule")
            putString("pantanal.capsule.type", "stopwatch")
            putBoolean("key_is_live_notification", true)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_mic)
            .setContentTitle("DJI Mic 正在录音")
            .setContentText("高清蓝牙音频录制中 • 点击返回")
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH) // 触发流体云左上角实时胶囊计时
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setUsesChronometer(true)
            .setChronometerCountDown(false)
            .setWhen(recordingStartTime)
            .setShowWhen(true)
            .addExtras(fluidCloudExtras)
            .addAction(
                android.R.drawable.ic_media_pause,
                "停止录音",
                stopPendingIntent
            )

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }
}
