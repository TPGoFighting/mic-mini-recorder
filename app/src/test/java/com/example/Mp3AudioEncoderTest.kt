package com.example

import com.example.audio.Mp3AudioEncoder
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.PI
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Mp3AudioEncoderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testMp3AudioEncoderRecordingPipeline() {
        val sampleRate = 16000
        val encoder = Mp3AudioEncoder(sampleRate = sampleRate, channelCount = 1, bitrateKbps = 64)
        val outputFile = tempFolder.newFile("recorded_test.mp3")

        encoder.start(outputFile)

        // Simulate 2 seconds of audio buffer writes in 100ms chunks (1600 samples per chunk)
        val chunkSize = 1600
        val totalSeconds = 2
        val totalSamples = sampleRate * totalSeconds

        val pcm = ShortArray(chunkSize)
        var sampleIndex = 0

        for (chunk in 0 until (totalSamples / chunkSize)) {
            for (i in 0 until chunkSize) {
                pcm[i] = (sin(2.0 * PI * 440.0 * sampleIndex / sampleRate) * Short.MAX_VALUE * 0.7).toInt().toShort()
                sampleIndex++
            }
            encoder.encode(pcm, chunkSize)
        }

        encoder.finish()
        encoder.close()

        println("Generated MP3 file path: ${outputFile.absolutePath}, size: ${outputFile.length()} bytes")
        assertTrue("Generated MP3 file must not be empty", outputFile.length() > 0)
        assertTrue("Generated MP3 should have realistic bitrate size (>= 8KB for 2s at 64kbps)", outputFile.length() >= 8000)
    }
}
