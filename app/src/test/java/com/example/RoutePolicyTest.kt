package com.example

import android.media.AudioDeviceInfo
import com.example.audio.RoutePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun bluetoothA2dpIsRejectedAsExternalInput() {
        assertFalse(RoutePolicy.isExternalBluetooth(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP))
    }

    @Test
    fun bleSpeakerIsRejectedAsExternalInput() {
        assertFalse(RoutePolicy.isExternalBluetooth(26))
    }

    @Test
    fun builtinMicIsRejected() {
        assertFalse(RoutePolicy.isExternalBluetooth(AudioDeviceInfo.TYPE_BUILTIN_MIC))
    }

    @Test
    fun getDeviceTypeNameReturnsReadableLabels() {
        assertEquals("蓝牙 SCO (DJI Mic)", RoutePolicy.getDeviceTypeName(AudioDeviceInfo.TYPE_BLUETOOTH_SCO))
        assertEquals("手机内置麦克风", RoutePolicy.getDeviceTypeName(AudioDeviceInfo.TYPE_BUILTIN_MIC))
        assertEquals("BLE 蓝牙耳机/麦克风", RoutePolicy.getDeviceTypeName(AudioDeviceInfo.TYPE_BLE_HEADSET))
    }
}
