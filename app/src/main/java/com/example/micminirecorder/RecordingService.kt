package com.example.micminirecorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.naman14.androidlame.AndroidLame
import com.naman14.androidlame.LameBuilder
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class RecordingService : Service() {
    private val stopRequested = AtomicBoolean(false)
    private lateinit var routeController: AudioRouteController
    private var worker: Thread? = null
    @Volatile private var activeRecord: AudioRecord? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        routeController = AudioRouteController(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRecording()
            return START_NOT_STICKY
        }

        startAsForeground("正在查找 DJI Mic Mini 蓝牙麦克风…")
        if (worker == null) {
            stopRequested.set(false)
            worker = Thread(::captureLoop, "mic-mini-capture").also { it.start() }
        }
        return START_NOT_STICKY
    }

    private fun captureLoop() {
        acquireWakeLock()
        try {
            while (!stopRequested.get()) {
                val device = routeController.selectBluetoothCommunicationDevice()
                if (device == null) {
                    updateNotification("等待 DJI Mic Mini 蓝牙麦克风…")
                    sleepQuietly(RECONNECT_DELAY_MS)
                    continue
                }

                updateNotification("已连接 ${device.productName}，正在验证外部输入…")
                captureOneSegment()
                sleepQuietly(RECONNECT_DELAY_MS)
            }
        } finally {
            activeRecord = null
            routeController.release()
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun captureOneSegment() {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) {
            updateNotification("当前设备不支持 16 kHz 蓝牙录音。")
            sleepQuietly(RECONNECT_DELAY_MS)
            return
        }

        val record = try {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build(),
                )
                .setBufferSizeInBytes((minBuffer * 2).coerceAtLeast(BUFFER_BYTES))
                .build()
        } catch (error: Exception) {
            updateNotification("无法创建蓝牙录音输入：${error.javaClass.simpleName}")
            return
        }

        activeRecord = record
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            updateNotification("蓝牙录音输入初始化失败。")
            return
        }

        try {
            record.startRecording()
            if (!routeController.hasExternalInput(record)) {
                updateNotification("已拒绝录音：当前不是 DJI 外部麦克风。")
                return
            }

            val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "MicMini")
            outputDir.mkdirs()
            val outputFile = File(outputDir, SegmentName.create())
            updateNotification("录音中：${routeController.routeLabel(record)}")
            segmentStartedAt = SystemClock.elapsedRealtime()
            writeMp3(record, outputFile)
        } catch (error: Exception) {
            updateNotification("录音中断，将尝试重新连接：${error.javaClass.simpleName}")
        } finally {
            activeRecord = null
            runCatching { record.stop() }
            record.release()
        }
    }

    private fun writeMp3(record: AudioRecord, outputFile: File) {
        val lame: AndroidLame = LameBuilder()
            .setInSampleRate(SAMPLE_RATE)
            .setOutChannels(1)
            .setOutBitrate(MP3_BITRATE_KBPS)
            .setOutSampleRate(SAMPLE_RATE)
            .setMode(LameBuilder.Mode.MONO)
            .setQuality(5)
            .build()

        val pcm = ShortArray(PCM_SAMPLES)
        val mp3 = ByteArray(MP3_BUFFER_BYTES)
        try {
            FileOutputStream(outputFile).use { output ->
                while (!stopRequested.get() &&
                    SystemClock.elapsedRealtime() - segmentStartedAt < SEGMENT_DURATION_MS
                ) {
                    val read = record.read(pcm, 0, pcm.size, AudioRecord.READ_BLOCKING)
                    if (read <= 0) {
                        if (read == AudioRecord.ERROR_DEAD_OBJECT || read == AudioRecord.ERROR_INVALID_OPERATION) {
                            throw IllegalStateException("AudioRecord read error $read")
                        }
                        continue
                    }
                    val encoded = lame.encodeBufferInterLeaved(pcm, read, mp3)
                    if (encoded > 0) output.write(mp3, 0, encoded)
                }
                val flushed = lame.flush(mp3)
                if (flushed > 0) output.write(mp3, 0, flushed)
            }
        } finally {
            lame.close()
        }
    }

    private var segmentStartedAt: Long = 0L

    private fun startAsForeground(text: String) {
        val notification = buildNotification(text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(text: String) {
        startAsForeground(text)
    }

    private fun buildNotification(text: String): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, RecordingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(getString(com.example.micminirecorder.R.string.notification_title))
            .setContentText(text)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, "停止", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(com.example.micminirecorder.R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    private fun acquireWakeLock() {
        val manager = getSystemService(PowerManager::class.java)
        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:recording").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun stopRecording() {
        stopRequested.set(true)
        activeRecord?.let { runCatching { it.stop() } }
        worker?.interrupt()
        stopSelf()
    }

    private fun sleepQuietly(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.micminirecorder.START"
        const val ACTION_STOP = "com.example.micminirecorder.STOP"

        private const val CHANNEL_ID = "micmini_recording"
        private const val NOTIFICATION_ID = 4201
        private const val SAMPLE_RATE = 16_000
        private const val MP3_BITRATE_KBPS = 64
        private const val PCM_SAMPLES = 4_096
        private const val MP3_BUFFER_BYTES = 16_384
        private const val BUFFER_BYTES = 16_384
        private const val SEGMENT_DURATION_MS = 30 * 60 * 1_000L
        private const val RECONNECT_DELAY_MS = 2_000L
    }
}
