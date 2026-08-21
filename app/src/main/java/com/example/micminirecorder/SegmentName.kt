package com.example.micminirecorder

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SegmentName {
    private val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun create(timestampMillis: Long = System.currentTimeMillis()): String =
        "micmini_${formatter.format(Date(timestampMillis))}.mp3"
}
