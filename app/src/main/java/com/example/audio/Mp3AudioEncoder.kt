package com.example.audio

import android.util.Log
import de.sciss.jump3r.lowlevel.LameEncoder
import java.io.File
import java.io.FileOutputStream
import javax.sound.sampled.AudioFormat

class Mp3AudioEncoder(
    private val sampleRate: Int = 16000,
    private val channelCount: Int = 1,
    private val bitrateKbps: Int = 64
) : AudioEncoder {

    companion object {
        private const val TAG = "Mp3AudioEncoder"
    }

    private var outputStream: FileOutputStream? = null
    private var encoder: LameEncoder? = null
    private var mp3Buffer: ByteArray = ByteArray(0)
    private var pcmByteBuffer: ByteArray = ByteArray(0)

    override fun start(outputFile: File) {
        outputStream = FileOutputStream(outputFile)
        val audioFormat = AudioFormat(
            sampleRate.toFloat(),
            16,
            channelCount,
            true,  // signed
            false  // little-endian
        )
        val channelMode = if (channelCount == 1) {
            LameEncoder.CHANNEL_MODE_MONO
        } else {
            LameEncoder.CHANNEL_MODE_STEREO
        }
        val enc = LameEncoder(
            audioFormat,
            bitrateKbps,
            channelMode,
            LameEncoder.QUALITY_MIDDLE,
            false
        )
        encoder = enc
        mp3Buffer = ByteArray(enc.getMP3BufferSize().coerceAtLeast(8192))
        pcmByteBuffer = ByteArray(4096 * 2)
        Log.i(TAG, "Mp3AudioEncoder started: sampleRate=$sampleRate, channels=$channelCount, bitrate=${bitrateKbps}kbps")
    }

    override fun encode(pcmData: ShortArray, samplesRead: Int) {
        val enc = encoder ?: return
        val fos = outputStream ?: return

        if (samplesRead <= 0) return

        val requiredBytes = samplesRead * 2
        if (pcmByteBuffer.size < requiredBytes) {
            pcmByteBuffer = ByteArray(requiredBytes)
        }

        // Convert 16-bit PCM short array to little-endian byte array
        var byteIdx = 0
        for (i in 0 until samplesRead) {
            val sample = pcmData[i].toInt()
            pcmByteBuffer[byteIdx++] = (sample and 0xFF).toByte()
            pcmByteBuffer[byteIdx++] = ((sample ushr 8) and 0xFF).toByte()
        }

        try {
            val bytesEncoded = enc.encodeBuffer(pcmByteBuffer, 0, requiredBytes, mp3Buffer)
            if (bytesEncoded > 0) {
                fos.write(mp3Buffer, 0, bytesEncoded)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding MP3 buffer", e)
        }
    }

    override fun finish() {
        val enc = encoder ?: return
        val fos = outputStream ?: return

        try {
            val bytesFlushed = enc.encodeFinish(mp3Buffer)
            if (bytesFlushed > 0) {
                fos.write(mp3Buffer, 0, bytesFlushed)
            }
            fos.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error flushing MP3 encoder", e)
        }
    }

    override fun close() {
        try {
            encoder?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing LameEncoder", e)
        } finally {
            encoder = null
            outputStream?.runCatching { close() }
            outputStream = null
        }
    }
}
