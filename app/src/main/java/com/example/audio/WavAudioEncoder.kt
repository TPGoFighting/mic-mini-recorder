package com.example.audio

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavAudioEncoder(
    private val sampleRate: Int = 16_000,
    private val channelCount: Int = 1,
    private val bitsPerSample: Int = 16
) : AudioEncoder {

    private var file: File? = null
    private var randomAccessFile: RandomAccessFile? = null
    private var totalAudioLen: Long = 0

    override fun start(outputFile: File) {
        this.file = outputFile
        this.totalAudioLen = 0
        val raf = RandomAccessFile(outputFile, "rw")
        raf.setLength(0)
        // Write placeholder 44-byte WAV header
        val header = ByteArray(44)
        raf.write(header)
        this.randomAccessFile = raf
    }

    override fun encode(pcmData: ShortArray, samplesRead: Int) {
        val raf = randomAccessFile ?: return
        if (samplesRead <= 0) return

        val byteBuffer = ByteBuffer.allocate(samplesRead * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until samplesRead) {
            byteBuffer.putShort(pcmData[i])
        }
        val bytes = byteBuffer.array()
        raf.write(bytes)
        totalAudioLen += bytes.size
    }

    override fun finish() {
        val raf = randomAccessFile ?: return
        val totalDataLen = totalAudioLen + 36
        val byteRate = (sampleRate * channelCount * bitsPerSample / 8).toLong()

        val header = createWavHeader(totalAudioLen, totalDataLen, sampleRate.toLong(), channelCount, byteRate)
        raf.seek(0)
        raf.write(header)
    }

    private fun createWavHeader(
        totalAudioLen: Long,
        totalDataLen: Long,
        longSampleRate: Long,
        channels: Int,
        byteRate: Long
    ): ByteArray {
        val header = ByteArray(44)
        // RIFF/WAVE header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xffL).toByte()
        header[5] = (totalDataLen shr 8 and 0xffL).toByte()
        header[6] = (totalDataLen shr 16 and 0xffL).toByte()
        header[7] = (totalDataLen shr 24 and 0xffL).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        // 'fmt ' chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1 (PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xffL).toByte()
        header[25] = (longSampleRate shr 8 and 0xffL).toByte()
        header[26] = (longSampleRate shr 16 and 0xffL).toByte()
        header[27] = (longSampleRate shr 24 and 0xffL).toByte()
        header[28] = (byteRate and 0xffL).toByte()
        header[29] = (byteRate shr 8 and 0xffL).toByte()
        header[30] = (byteRate shr 16 and 0xffL).toByte()
        header[31] = (byteRate shr 24 and 0xffL).toByte()
        header[32] = (channels * 2).toByte() // block align
        header[33] = 0
        header[34] = bitsPerSample.toByte() // bits per sample
        header[35] = 0
        // 'data' chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xffL).toByte()
        header[41] = (totalAudioLen shr 8 and 0xffL).toByte()
        header[42] = (totalAudioLen shr 16 and 0xffL).toByte()
        header[43] = (totalAudioLen shr 24 and 0xffL).toByte()
        return header
    }

    override fun close() {
        try {
            randomAccessFile?.close()
        } catch (_: Exception) {}
        randomAccessFile = null
    }
}
