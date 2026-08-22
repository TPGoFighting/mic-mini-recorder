package com.example

import com.example.audio.AudioUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioUtilsTest {

    @Test
    fun calculateRmsDbReturnsSilenceForZeroBuffer() {
        val buffer = ShortArray(1024) { 0 }
        val db = AudioUtils.calculateRmsDb(buffer, buffer.size)
        assertEquals(-96f, db, 0.1f)
    }

    @Test
    fun calculateRmsDbReturnsNegativeDbForNormalSignal() {
        val buffer = ShortArray(1024) { 1000 }
        val db = AudioUtils.calculateRmsDb(buffer, buffer.size)
        assertTrue(db < 0f && db > -96f)
    }

    @Test
    fun calculatePeakNormalizedReturnsOneForMaxSignal() {
        val buffer = shortArrayOf(0, 100, -32768, 500)
        val peak = AudioUtils.calculatePeakNormalized(buffer, buffer.size)
        assertEquals(1.0f, peak, 0.01f)
    }

    @Test
    fun formatDurationFormatsSecondsAndHours() {
        assertEquals("00:45", AudioUtils.formatDuration(45_000L))
        assertEquals("05:30", AudioUtils.formatDuration(330_000L))
        assertEquals("01:15:20", AudioUtils.formatDuration(4520_000L))
    }

    @Test
    fun formatFileSizeFormatsBytesKbMb() {
        assertEquals("500 B", AudioUtils.formatFileSize(500L))
        assertEquals("10.0 KB", AudioUtils.formatFileSize(10_240L))
        assertEquals("2.50 MB", AudioUtils.formatFileSize((2.5 * 1024 * 1024).toLong()))
    }
}
