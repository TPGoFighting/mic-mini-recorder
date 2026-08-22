package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioRouteController
import com.example.audio.AudioUtils
import com.example.audio.SegmentName
import com.example.audio.WavAudioEncoder
import com.example.data.RecordingsRepository
import com.example.model.AudioFormatType
import com.example.model.RecordingConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TP Recorder", appName)
    }

    @Test
    fun recordingsRepositoryDirectoryCreation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = RecordingsRepository(context)
        val dir = repository.getRecordingsDir()
        assertNotNull(dir)
        assertTrue(dir.exists() || dir.mkdirs())
    }

    @Test
    fun recordingConfigDefaults() {
        val config = RecordingConfig()
        assertEquals(30, config.segmentDurationMinutes)
        assertEquals(30 * 60 * 1000L, config.segmentDurationMs)
        assertEquals(AudioFormatType.MP3, config.format)
    }

    @Test
    fun audioUtilsCalculations() {
        val silentPcm = ShortArray(100) { 0 }
        val silentRms = AudioUtils.calculateRmsDb(silentPcm, 100)
        assertEquals(-96f, silentRms, 0.1f)

        val silentPeak = AudioUtils.calculatePeakNormalized(silentPcm, 100)
        assertEquals(0f, silentPeak, 0.01f)

        val fullScalePcm = ShortArray(100) { 32767 }
        val fullRms = AudioUtils.calculateRmsDb(fullScalePcm, 100)
        assertTrue(fullRms > -1f)

        val formattedDuration = AudioUtils.formatDuration(3661_000L)
        assertEquals("01:01:01", formattedDuration)

        val formattedSize = AudioUtils.formatFileSize(1024 * 1024 * 5)
        assertEquals("5.00 MB", formattedSize)
    }

    @Test
    fun segmentNameGenerationAndParsing() {
        val now = 1700000000000L
        val name = SegmentName.create(now, "wav")
        assertTrue(name.startsWith("micmini_"))
        assertTrue(name.endsWith(".wav"))

        val parsed = SegmentName.parseTimestamp(name)
        assertNotNull(parsed)
    }

    @Test
    fun wavEncoderCreatesValidHeader() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testFile = File(context.cacheDir, "test_out.wav")
        if (testFile.exists()) testFile.delete()

        val encoder = WavAudioEncoder(sampleRate = 16000, channelCount = 1, bitsPerSample = 16)
        encoder.start(testFile)

        val samples = ShortArray(1600) { 1000 }
        encoder.encode(samples, 1600)
        encoder.finish()
        encoder.close()

        assertTrue(testFile.exists())
        assertEquals(44L + 1600 * 2, testFile.length())
        testFile.delete()
    }

    @Test
    fun audioRouteControllerReturnsDevices() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = AudioRouteController(context)
        val devices = controller.getAvailableInputDevices()
        assertTrue(devices.isNotEmpty())
        assertEquals(0, devices.first().id) // Auto option
        controller.release()
    }
}

