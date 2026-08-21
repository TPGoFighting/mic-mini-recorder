package com.example.micminirecorder

import android.media.AudioDeviceInfo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoutePolicyTest {
    @Test
    fun bluetoothScoIsAcceptedAsExternalInput() {
        assertTrue(RoutePolicy.isExternalBluetooth(AudioDeviceInfo.TYPE_BLUETOOTH_SCO))
    }

    @Test
    fun bleHeadsetIsAcceptedAsExternalInput() {
        assertTrue(RoutePolicy.isExternalBluetooth(AudioDeviceInfo.TYPE_BLE_HEADSET))
    }

    @Test
    fun builtinMicIsRejected() {
        assertFalse(RoutePolicy.isExternalBluetooth(AudioDeviceInfo.TYPE_BUILTIN_MIC))
    }
}
