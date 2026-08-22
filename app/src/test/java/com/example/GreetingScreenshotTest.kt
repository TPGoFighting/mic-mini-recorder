package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.AudioDeviceItem
import com.example.model.AudioFormatType
import com.example.model.AudioRouteInfo
import com.example.model.RecordingConfig
import com.example.model.RecordingFileItem
import com.example.model.RecordingState
import com.example.player.PlayerState
import com.example.service.RecordingServiceUiState
import com.example.ui.RecorderTabContent
import com.example.ui.components.RecordingsList
import com.example.ui.components.SettingsContent
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun settings_page_screenshot() {
        val sampleDevices = listOf(
            AudioDeviceItem(id = 0, name = "智能自动选择 (推荐)", type = 0, typeName = "自动", isBluetooth = false, isDefault = true),
            AudioDeviceItem(id = 2, name = "DJI Mic Mini (SCO)", type = 7, typeName = "蓝牙 SCO 麦克风", isBluetooth = true),
            AudioDeviceItem(id = 1, name = "手机内置麦克风", type = 15, typeName = "内置麦克风", isBluetooth = false)
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                SettingsContent(
                    config = RecordingConfig(
                        strictBluetoothOnly = true,
                        segmentDurationMinutes = 30,
                        sampleRate = 16000,
                        bitrateKbps = 128,
                        format = AudioFormatType.MP3,
                        preferredDeviceId = 2
                    ),
                    availableDevices = sampleDevices,
                    onConfigChange = {},
                    onRefreshDevices = {},
                    storagePath = "/storage/emulated/0/Android/data/com.example/files/Music/MicMini",
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/settings_page.png")
    }

    @Test
    fun recorder_main_screen_screenshot() {
        val sampleRoute = AudioRouteInfo(
            deviceName = "sdk_gphone64_arm64",
            deviceType = 15,
            deviceId = 1,
            isExternalBluetooth = false,
            isFallback = false,
            description = "当前启用输出: sdk_gphone64_arm64"
        )

        val serviceState = RecordingServiceUiState(
            state = RecordingState.IDLE,
            elapsedDurationMs = 0L,
            currentSegmentFileName = "",
            segmentIndex = 0,
            statusMessage = "就绪",
            recentWaveform = listOf(0.15f, 0.35f, 0.6f, 0.85f, 0.7f, 0.45f, 0.65f, 0.9f, 0.5f, 0.3f, 0.2f),
            currentRmsDb = -96f,
            routeInfo = sampleRoute
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                RecorderTabContent(
                    serviceState = serviceState,
                    routeInfo = sampleRoute,
                    config = RecordingConfig(),
                    isRecordingOrBusy = false,
                    isActuallyRecording = false,
                    isPaused = false,
                    onStart = {},
                    onStop = {},
                    onPause = {},
                    onResume = {},
                    onRefreshRoute = {},
                    onOpenSettings = {},
                    onSelectMicClicked = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/recorder_main.png")
    }

    @Test
    fun recordings_library_screenshot() {
        val sampleRecordings = listOf(
            RecordingFileItem(
                path = "/storage/emulated/0/Music/MicMini/REC_20260822_120000_seg01.mp3",
                name = "REC_20260822_120000_seg01.mp3",
                sizeBytes = 14_680_064L,
                durationMs = 1800_000L,
                lastModifiedMs = System.currentTimeMillis() - 3600_000L
            ),
            RecordingFileItem(
                path = "/storage/emulated/0/Music/MicMini/REC_20260822_113000_seg01.mp3",
                name = "REC_20260822_113000_seg01.mp3",
                sizeBytes = 14_680_064L,
                durationMs = 1800_000L,
                lastModifiedMs = System.currentTimeMillis() - 7200_000L
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                RecordingsList(
                    recordings = sampleRecordings,
                    playerState = PlayerState(
                        isPlaying = true,
                        currentFilePath = sampleRecordings.first().path,
                        currentPositionMs = 345_000L,
                        totalDurationMs = 1800_000L
                    ),
                    onPlay = {},
                    onPause = {},
                    onResume = {},
                    onSeek = {},
                    onDelete = {},
                    onRefresh = {},
                    onShare = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/recordings_library.png")
    }
}
