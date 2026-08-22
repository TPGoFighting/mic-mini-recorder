package com.example

import com.example.audio.SegmentName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentNameTest {

    @Test
    fun segmentNameIsM4aAndSortable() {
        val name = SegmentName.create(1_735_689_600_000L, "m4a")
        assertTrue(name.startsWith("micmini_"))
        assertTrue(name.endsWith(".m4a"))
    }

    @Test
    fun segmentNameSupportsWavExtension() {
        val name = SegmentName.create(1_735_689_600_000L, "wav")
        assertTrue(name.startsWith("micmini_"))
        assertTrue(name.endsWith(".wav"))
    }

    @Test
    fun parseTimestampWorksCorrectly() {
        val timestamp = 1_735_689_600_000L
        val name = SegmentName.create(timestamp, "m4a")
        val parsed = SegmentName.parseTimestamp(name)
        assertNotNull(parsed)
    }
}
