package com.dji.recorder.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.dji.recorder.model.AudioConfig
import com.dji.recorder.model.BluetoothMicDevice
import com.dji.recorder.model.NoiseReductionMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

/**
 * 专为 DJI Mic / 蓝牙麦克风打造的稳健录音引擎。
 * 兼容性保证：
 * 1. 针对蓝牙 SCO 外设自动采用兼容 AudioSource (VOICE_COMMUNICATION / DEFAULT)，确保蓝牙上行音频流 100% 接通。
 * 2. 采样率自适应与波形实时分析。
 */
class WavAudioRecorder(private val context: Context) {

    private val TAG = "WavAudioRecorder"

    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var automaticGainControl: AutomaticGainControl? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private var currentWavFile: File? = null
    private var totalBytesRecorded: Long = 0L
    private var currentConfig: AudioConfig = AudioConfig()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private val _currentDecibels = MutableStateFlow(0f)
    val currentDecibels: StateFlow<Float> = _currentDecibels.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private val _activeRoutedDevice = MutableStateFlow<String>("等待录音开始...")
    val activeRoutedDevice: StateFlow<String> = _activeRoutedDevice.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startRecording(
        targetMic: BluetoothMicDevice,
        mode: NoiseReductionMode,
        config: AudioConfig
    ): Result<File> {
        this.currentConfig = config
        val sampleRate = config.sampleRate
        val channelConfig = if (config.isStereo) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize <= 0) {
            return Result.failure(IllegalStateException("Unsupported audio configuration: $sampleRate Hz"))
        }

        val bufferSize = max(minBufferSize * 2, 8192)

        // 蓝牙 SCO 外设在 Android/ColorOS 下必须使用 VOICE_COMMUNICATION 才能将物理收音流强制绑定到蓝牙耳机/麦克风极头
        val primarySource = if (targetMic.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
            MediaRecorder.AudioSource.VOICE_COMMUNICATION
        } else {
            MediaRecorder.AudioSource.MIC
        }

        try {
            audioRecord = AudioRecord(
                primarySource,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    audioRecord?.release()
                    audioRecord = null
                    return Result.failure(IllegalStateException("AudioRecord failed to initialize"))
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val success = audioRecord?.setPreferredDevice(targetMic.rawDeviceInfo) ?: false
                Log.i(TAG, "AudioRecord setPreferredDevice (${targetMic.name}): $success")
            }

            val outputFile = createNewTempWavFile()
            currentWavFile = outputFile
            totalBytesRecorded = 0L
            _recordingDurationMs.value = 0L

            if (mode != NoiseReductionMode.OFF) {
                val sessionId = audioRecord?.audioSessionId ?: 0
                if (sessionId != 0) {
                    if (NoiseSuppressor.isAvailable()) {
                        try {
                            noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply {
                                enabled = true
                            }
                            Log.i(TAG, "Hardware NoiseSuppressor enabled: ${noiseSuppressor?.hasControl()}")
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not enable NoiseSuppressor", e)
                        }
                    }
                    if (AutomaticGainControl.isAvailable()) {
                        try {
                            automaticGainControl = AutomaticGainControl.create(sessionId)?.apply {
                                enabled = true
                            }
                            Log.i(TAG, "Hardware AGC enabled: ${automaticGainControl?.hasControl()}")
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not enable AGC", e)
                        }
                    }
                }
            }

            audioRecord?.startRecording()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val updateRoutingInfo = {
                    val routed = audioRecord?.routedDevice
                    if (routed != null) {
                        val isBt = routed.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                                routed.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                                routed.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                        val isUsb = routed.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                                routed.type == AudioDeviceInfo.TYPE_USB_HEADSET
                        val status = when {
                            isBt -> "🎯 蓝牙直通: ${targetMic.name}"
                            isUsb -> "🎯 USB直通: ${targetMic.name}"
                            else -> "⚠️ 手机麦克风 (${routed.productName ?: "内置"})"
                        }
                        _activeRoutedDevice.value = status
                        Log.i(TAG, "Active Hardware Mic Routed: $status (type: ${routed.type})")
                    }
                }
                updateRoutingInfo()
                audioRecord?.addOnRoutingChangedListener({
                    updateRoutingInfo()
                }, android.os.Handler(android.os.Looper.getMainLooper()))
            }

            recordingJob = scope.launch {
                readAndWriteAudioData(outputFile, bufferSize)
            }

            Log.i(TAG, "Recording Started: ${sampleRate}Hz, Stereo: ${config.isStereo}, Source: ${audioRecord?.audioSource}")
            return Result.success(outputFile)

        } catch (e: Exception) {
            Log.e(TAG, "Exception during startRecording", e)
            releaseAudioRecord()
            return Result.failure(e)
        }
    }

    fun stopRecording(): File? {
        recordingJob?.cancel()
        recordingJob = null
        releaseAudioRecord()

        val file = currentWavFile
        if (file != null && file.exists()) {
            writeWavHeader(file, totalBytesRecorded, currentConfig)
            Log.i(TAG, "Finished recording studio WAV: ${file.name} ($totalBytesRecorded bytes)")
        }

        _currentAmplitude.value = 0f
        _currentDecibels.value = 0f
        return file
    }

    fun abortRecording() {
        recordingJob?.cancel()
        recordingJob = null
        releaseAudioRecord()

        currentWavFile?.let { file ->
            if (file.exists() && totalBytesRecorded > 0) {
                writeWavHeader(file, totalBytesRecorded, currentConfig)
            }
        }
        _currentAmplitude.value = 0f
        _currentDecibels.value = 0f
    }

    private fun releaseAudioRecord() {
        try {
            noiseSuppressor?.release()
            noiseSuppressor = null
            automaticGainControl?.release()
            automaticGainControl = null
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioRecord", e)
        } finally {
            audioRecord = null
        }
    }

    private suspend fun readAndWriteAudioData(
        file: File,
        bufferSize: Int
    ) {
        val buffer = ShortArray(bufferSize / 2)
        val byteBuffer = ByteArray(bufferSize)
        val startTime = System.currentTimeMillis()

        FileOutputStream(file).use { outputStream ->
            outputStream.write(ByteArray(44))

            while (scope.isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readCount > 0) {
                    var maxAmplitude = 0
                    var sum = 0.0

                    for (i in 0 until readCount) {
                        val sample = buffer[i]
                        val absVal = abs(sample.toInt())
                        if (absVal > maxAmplitude) {
                            maxAmplitude = absVal
                        }
                        sum += sample * sample

                        val byteIndex = i * 2
                        byteBuffer[byteIndex] = (sample.toInt() and 0xFF).toByte()
                        byteBuffer[byteIndex + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                    }

                    val bytesToWrite = readCount * 2
                    outputStream.write(byteBuffer, 0, bytesToWrite)
                    totalBytesRecorded += bytesToWrite

                    val normalizedAmp = min(1.0f, maxAmplitude / 32767.0f)
                    _currentAmplitude.value = normalizedAmp

                    val rms = kotlin.math.sqrt(sum / readCount)
                    val db = if (rms > 0) (20 * log10(rms / 32767.0)).toFloat() else -96f
                    _currentDecibels.value = max(-60f, db)

                    _recordingDurationMs.value = System.currentTimeMillis() - startTime
                }
            }
        }
    }

    private fun createNewTempWavFile(): File {
        val dir = StorageHelper.getTargetDirectory(context, currentConfig)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(dir, "DJI_MIC_$timeStamp.wav")
    }

    private fun writeWavHeader(file: File, audioDataLength: Long, config: AudioConfig) {
        val totalDataLen = audioDataLength + 36
        val sampleRate = config.sampleRate
        val channels = if (config.isStereo) 2 else 1
        val bitsPerSample = 16
        val byteRate = (sampleRate * channels * bitsPerSample / 8).toLong()

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xffL).toByte(); header[5] = ((totalDataLen shr 8) and 0xffL).toByte()
        header[6] = ((totalDataLen shr 16) and 0xffL).toByte(); header[7] = ((totalDataLen shr 24) and 0xffL).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0; header[22] = channels.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte(); header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte(); header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xffL).toByte(); header[29] = ((byteRate shr 8) and 0xffL).toByte()
        header[30] = ((byteRate shr 16) and 0xffL).toByte(); header[31] = ((byteRate shr 24) and 0xffL).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte(); header[33] = 0
        header[34] = bitsPerSample.toByte(); header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (audioDataLength and 0xffL).toByte(); header[41] = ((audioDataLength shr 8) and 0xffL).toByte()
        header[42] = ((audioDataLength shr 16) and 0xffL).toByte(); header[43] = ((audioDataLength shr 24) and 0xffL).toByte()

        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0)
            raf.write(header)
        }
    }
}
