package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.AudioEncoder
import com.example.audio.AudioRouteController
import com.example.audio.AudioUtils
import com.example.audio.MediaCodecAudioEncoder
import com.example.audio.Mp3AudioEncoder
import com.example.audio.SegmentName
import com.example.audio.WavAudioEncoder
import com.example.model.AudioFormatType
import com.example.model.AudioRouteInfo
import com.example.model.RecordingConfig
import com.example.model.RecordingState
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class RecordingService : Service() {

    private lateinit var routeController: AudioRouteController
    private var wakeLock: PowerManager.WakeLock? = null
    private var worker: Thread? = null
    private val stopRequested = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private var activeRecord: AudioRecord? = null

    private var config = RecordingConfig()
    private var totalRecordedDurationMs: Long = 0L
    private var segmentCount: Int = 0

    companion object {
        private const val TAG = "RecordingService"
        const val ACTION_START = "com.example.micminirecorder.ACTION_START"
        const val ACTION_STOP = "com.example.micminirecorder.ACTION_STOP"
        const val ACTION_PAUSE = "com.example.micminirecorder.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.micminirecorder.ACTION_RESUME"

        const val EXTRA_STRICT_BT = "extra_strict_bt"
        const val EXTRA_SEGMENT_MINUTES = "extra_segment_minutes"
        const val EXTRA_SAMPLE_RATE = "extra_sample_rate"
        const val EXTRA_BITRATE_KBPS = "extra_bitrate_kbps"
        const val EXTRA_FORMAT = "extra_format"
        const val EXTRA_PREFERRED_DEVICE_ID = "extra_preferred_device_id"

        private const val CHANNEL_ID = "micmini_recording_channel"
        private const val NOTIFICATION_ID = 4201
        private const val RECONNECT_DELAY_MS = 1_500L
        private const val PCM_FRAME_SIZE = 2048
    }

    override fun onCreate() {
        super.onCreate()
        routeController = AudioRouteController(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                stopRecording()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                isPaused.set(true)
                RecordingServiceBus.update { it.copy(state = RecordingState.PAUSED, statusMessage = "录音已暂停") }
                updateNotification("录音已暂停")
                return START_STICKY
            }
            ACTION_RESUME -> {
                isPaused.set(false)
                RecordingServiceBus.update { it.copy(state = RecordingState.RECORDING, statusMessage = "录音已恢复") }
                updateNotification("正在录音中…")
                return START_STICKY
            }
            ACTION_START -> {
                // Safety invariant: this recorder never silently falls back to the phone mic.
                val strictBt = true
                val segmentMins = intent?.getIntExtra(EXTRA_SEGMENT_MINUTES, 30) ?: 30
                val sampleRate = intent?.getIntExtra(EXTRA_SAMPLE_RATE, 16_000) ?: 16_000
                val bitrateKbps = intent?.getIntExtra(EXTRA_BITRATE_KBPS, 64) ?: 64
                val formatName = intent?.getStringExtra(EXTRA_FORMAT) ?: AudioFormatType.MP3.name
                val format = try { AudioFormatType.valueOf(formatName) } catch (_: Exception) { AudioFormatType.MP3 }
                val preferredDeviceId = intent?.getIntExtra(EXTRA_PREFERRED_DEVICE_ID, 0) ?: 0

                config = RecordingConfig(
                    strictBluetoothOnly = strictBt,
                    segmentDurationMinutes = segmentMins,
                    sampleRate = sampleRate,
                    bitrateKbps = bitrateKbps,
                    format = format,
                    preferredDeviceId = preferredDeviceId
                )

                startAsForeground("正在初始化麦克风与音频路由…")

                if (worker == null || !worker!!.isAlive) {
                    stopRequested.set(false)
                    isPaused.set(false)
                    totalRecordedDurationMs = 0L
                    segmentCount = 0
                    worker = Thread(::captureMasterLoop, "MicMiniCaptureWorker").also { it.start() }
                }
            }
        }
        return START_STICKY
    }

    private fun captureMasterLoop() {
        acquireWakeLock()
        RecordingServiceBus.update {
            it.copy(
                state = RecordingState.SEARCHING_BLUETOOTH,
                statusMessage = "正在检测麦克风与音频路由…"
            )
        }

        try {
            while (!stopRequested.get()) {
                val selectedDevice = routeController.selectAudioDevice(config.preferredDeviceId)
                val routeInfo = routeController.getCurrentRouteInfo(activeRecord, config.preferredDeviceId)
                RecordingServiceBus.update { it.copy(routeInfo = routeInfo) }

                val isExternalBt = routeInfo.isExternalBluetooth

                if (config.strictBluetoothOnly && !isExternalBt) {
                    val waitMsg = "等待 DJI Mic Mini / 蓝牙麦克风连接…"
                    updateNotification(waitMsg)
                    RecordingServiceBus.update {
                        it.copy(
                            state = RecordingState.SEARCHING_BLUETOOTH,
                            statusMessage = waitMsg
                        )
                    }
                    sleepQuietly(RECONNECT_DELAY_MS)
                    continue
                }

                val statusText = "已选择输入: ${routeInfo.deviceName}"
                updateNotification(statusText)

                captureAudioSegment(isExternalBt)

                if (!stopRequested.get()) {
                    sleepQuietly(RECONNECT_DELAY_MS)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal capture loop error", e)
            RecordingServiceBus.update {
                it.copy(
                    state = RecordingState.ERROR,
                    statusMessage = "录音异常中断: ${e.message}",
                    lastError = e.message
                )
            }
        } finally {
            cleanupSession()
        }
    }

    private fun captureAudioSegment(hasBluetoothTarget: Boolean) {
        val sampleRate = config.sampleRate
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (minBufferSize <= 0) {
            val error = "系统不支持 ${sampleRate}Hz 音频采样"
            Log.e(TAG, error)
            RecordingServiceBus.update { it.copy(statusMessage = error, lastError = error) }
            sleepQuietly(RECONNECT_DELAY_MS)
            return
        }

        val bufferSize = Math.max(minBufferSize * 2, PCM_FRAME_SIZE * 4)
        if (!hasBluetoothTarget) {
            val rejectMsg = "已拒绝写入：当前没有合格的外部蓝牙输入"
            updateNotification(rejectMsg)
            RecordingServiceBus.update {
                it.copy(
                    state = RecordingState.SEARCHING_BLUETOOTH,
                    statusMessage = rejectMsg,
                    lastError = rejectMsg
                )
            }
            return
        }

        val audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION

        val record = try {
            val rec = AudioRecord.Builder()
                .setAudioSource(audioSource)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            // Apply preferred device binding if specified
            if (config.preferredDeviceId > 0) {
                routeController.applyPreferredDevice(rec, config.preferredDeviceId)
            }
            rec
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate AudioRecord", e)
            RecordingServiceBus.update { it.copy(statusMessage = "无法启动麦克风: ${e.message}") }
            return
        }

        activeRecord = record
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            activeRecord = null
            RecordingServiceBus.update { it.copy(statusMessage = "麦克风初始化失败") }
            return
        }

        try {
            record.startRecording()
            val currentRoute = routeController.getCurrentRouteInfo(record, config.preferredDeviceId)

            if (!routeController.hasExternalInput(record)) {
                val rejectMsg = "已拒绝写入：当前非 DJI/外部蓝牙麦克风输入"
                updateNotification(rejectMsg)
                RecordingServiceBus.update {
                    it.copy(
                        state = RecordingState.SEARCHING_BLUETOOTH,
                        routeInfo = currentRoute,
                        statusMessage = rejectMsg
                    )
                }
                return
            }

            val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "MicMini").apply { mkdirs() }
            val extension = config.format.extension
            val fileName = SegmentName.create(System.currentTimeMillis(), extension)
            val outputFile = File(outputDir, fileName)
            val tempFile = File(outputDir, "$fileName.part")

            segmentCount++
            val encoder: AudioEncoder = when (config.format) {
                AudioFormatType.MP3 -> Mp3AudioEncoder(
                    sampleRate = config.sampleRate,
                    channelCount = 1,
                    bitrateKbps = config.bitrateKbps
                )
                AudioFormatType.AAC_M4A -> MediaCodecAudioEncoder(
                    sampleRate = config.sampleRate,
                    channelCount = 1,
                    bitrate = config.bitrateKbps * 1000
                )
                AudioFormatType.WAV -> WavAudioEncoder(
                    sampleRate = config.sampleRate,
                    channelCount = 1,
                    bitsPerSample = 16
                )
            }

            // Never expose a file while the encoder is still writing it.
            encoder.start(tempFile)

            RecordingServiceBus.update {
                it.copy(
                    state = RecordingState.RECORDING,
                    routeInfo = currentRoute,
                    segmentIndex = segmentCount,
                    currentSegmentFileName = fileName,
                    statusMessage = "录音中: ${currentRoute.deviceName}"
                )
            }

            writeSegmentData(record, encoder, tempFile, outputFile)

        } catch (e: Exception) {
            Log.e(TAG, "Exception during segment capture", e)
            RecordingServiceBus.update { it.copy(statusMessage = "录音异常: ${e.message}") }
        } finally {
            activeRecord = null
            runCatching { record.stop() }
            runCatching { record.release() }
        }
    }

    private fun writeSegmentData(
        record: AudioRecord,
        encoder: AudioEncoder,
        tempFile: File,
        outputFile: File
    ) {
        val pcmBuffer = ShortArray(PCM_FRAME_SIZE)
        var lastActiveAt = SystemClock.elapsedRealtime()
        var recordedDurationMs = 0L
        var lastNotificationUpdate = 0L
        val recentWaveform = ArrayList<Float>(32).apply { repeat(32) { add(0.05f) } }
        var encoderFinished = false

        try {
            while (!stopRequested.get()) {
                val now = SystemClock.elapsedRealtime()
                val currentRoute = routeController.getCurrentRouteInfo(record, config.preferredDeviceId)
                if (!currentRoute.isExternalBluetooth) {
                    val lostMessage = "蓝牙输入已断开：停止写入，等待重新连接"
                    updateNotification(lostMessage)
                    RecordingServiceBus.update {
                        it.copy(
                            state = RecordingState.SEARCHING_BLUETOOTH,
                            routeInfo = currentRoute,
                            statusMessage = lostMessage,
                            lastError = null
                        )
                    }
                    break
                }

                if (isPaused.get()) {
                    // Do not count the pause gap toward the recording duration.
                    lastActiveAt = now
                    sleepQuietly(100)
                    continue
                }

                recordedDurationMs += (now - lastActiveAt).coerceAtLeast(0L)
                lastActiveAt = now
                if (recordedDurationMs >= config.segmentDurationMs) {
                    Log.d(TAG, "Segment duration reached (${config.segmentDurationMinutes} mins), rotating segment...")
                    break
                }

                val readCount = record.read(pcmBuffer, 0, pcmBuffer.size, AudioRecord.READ_BLOCKING)
                if (readCount <= 0) {
                    if (readCount == AudioRecord.ERROR_DEAD_OBJECT || readCount == AudioRecord.ERROR_INVALID_OPERATION) {
                        Log.w(TAG, "AudioRecord read error code: $readCount")
                        break
                    }
                    continue
                }

                // Encode PCM samples
                encoder.encode(pcmBuffer, readCount)

                // Calculate amplitude and dB
                val rmsDb = AudioUtils.calculateRmsDb(pcmBuffer, readCount)
                val peakAmp = AudioUtils.calculatePeakNormalized(pcmBuffer, readCount)

                if (recentWaveform.size >= 32) {
                    recentWaveform.removeAt(0)
                }
                recentWaveform.add(peakAmp.coerceAtLeast(0.04f))

                val totalDuration = totalRecordedDurationMs + recordedDurationMs

                // Update bus state
                RecordingServiceBus.update {
                    it.copy(
                        elapsedDurationMs = totalDuration,
                        currentRmsDb = rmsDb,
                        currentPeakAmp = peakAmp,
                        recentWaveform = recentWaveform.toList()
                    )
                }

                // Throttle notification text update to every 1 second
                val now = SystemClock.elapsedRealtime()
                if (now - lastNotificationUpdate >= 1000L) {
                    lastNotificationUpdate = now
                    val timeStr = AudioUtils.formatDuration(totalDuration)
                    val routeLabel = routeController.getRouteLabel(record)
                    updateNotification("[$timeStr] 分段 #$segmentCount ($routeLabel)")
                }
            }
        } finally {
            totalRecordedDurationMs += recordedDurationMs

            try {
                encoder.finish()
                encoderFinished = true
            } catch (e: Exception) {
                Log.e(TAG, "Error finishing encoder", e)
            }
            try {
                encoder.close()
            } catch (_: Exception) {}

            if (encoderFinished && tempFile.exists() && tempFile.length() > 0L) {
                if (outputFile.exists()) outputFile.delete()
                if (!tempFile.renameTo(outputFile)) {
                    Log.e(TAG, "Could not finalize segment: ${tempFile.absolutePath}")
                    tempFile.delete()
                }
            } else {
                tempFile.delete()
                Log.w(TAG, "Discarded incomplete segment: ${tempFile.absolutePath}")
            }

            Log.i(TAG, "Saved segment to: ${outputFile.absolutePath}, size: ${outputFile.length()} bytes")
        }
    }

    private fun cleanupSession() {
        activeRecord = null
        routeController.release()
        releaseWakeLock()

        RecordingServiceBus.update {
            it.copy(
                state = RecordingState.IDLE,
                statusMessage = "录音已停止",
                currentRmsDb = -96f,
                currentPeakAmp = 0f
            )
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startAsForeground(text: String) {
        val notification = buildNotification(text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(text: String): Notification {
        val activityIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, RecordingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(activityIntent)
            .addAction(android.R.drawable.ic_media_pause, getString(R.string.action_stop), stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val manager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:MicMiniCaptureLock").apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // Max 12 hours safety timeout
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun stopRecording() {
        stopRequested.set(true)
        activeRecord?.let { runCatching { it.stop() } }
        worker?.interrupt()
        worker = null
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
        cleanupSession()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
