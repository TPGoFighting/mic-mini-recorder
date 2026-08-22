package com.example.audio

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SegmentName {
    private val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun create(timestampMillis: Long = System.currentTimeMillis(), extension: String = "m4a"): String =
        "micmini_${formatter.format(Date(timestampMillis))}.$extension"

    fun parseTimestamp(fileName: String): Long? {
        val raw = fileName.removePrefix("micmini_").substringBeforeLast(".")
        return try {
            formatter.parse(raw)?.time
        } catch (_: Exception) {
            null
        }
    }
}
