package com.example.audio

import java.io.Closeable
import java.io.File

interface AudioEncoder : Closeable {
    fun start(outputFile: File)
    fun encode(pcmData: ShortArray, samplesRead: Int)
    fun finish()
}
