package com.example.micminirecorder

import android.media.AudioDeviceInfo

object RoutePolicy {
    fun isExternalBluetooth(type: Int): Boolean =
        type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            type == AudioDeviceInfo.TYPE_BLE_HEADSET
}
