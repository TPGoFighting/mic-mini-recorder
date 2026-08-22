package com.example.audio

import java.util.Locale
import kotlin.math.log10
import kotlin.math.sqrt

object AudioUtils {
    /**
     * Calculates Root Mean Square dBFS from 16-bit PCM samples.
     * Returns a value typically between -96 dB and 0 dB.
     */
    fun calculateRmsDb(pcm: ShortArray, samplesRead: Int): Float {
        if (samplesRead <= 0) return -96f
        var sumSquares = 0.0
        for (i in 0 until samplesRead) {
            val sample = pcm[i].toDouble()
            sumSquares += sample * sample
        }
        val rms = sqrt(sumSquares / samplesRead)
        if (rms <= 0.0) return -96f
        // Full scale 16-bit is 32767
        val db = 20 * log10(rms / 32767.0)
        return db.toFloat().coerceIn(-96f, 0f)
    }

    /**
     * Calculates peak amplitude normalized between 0.0f and 1.0f.
     */
    fun calculatePeakNormalized(pcm: ShortArray, samplesRead: Int): Float {
        if (samplesRead <= 0) return 0f
        var maxVal = 0
        for (i in 0 until samplesRead) {
            val absVal = Math.abs(pcm[i].toInt())
            if (absVal > maxVal) {
                maxVal = absVal
            }
        }
        return (maxVal / 32768f).coerceIn(0f, 1f)
    }

    fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
