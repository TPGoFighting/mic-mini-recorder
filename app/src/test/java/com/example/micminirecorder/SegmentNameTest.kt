package com.example.micminirecorder

import kotlin.test.Test
import kotlin.test.assertTrue

class SegmentNameTest {
    @Test
    fun segmentNameIsMp3AndSortable() {
        val name = SegmentName.create(1_735_689_600_000L)
        assertTrue(name.startsWith("micmini_"))
        assertTrue(name.endsWith(".mp3"))
    }
}
