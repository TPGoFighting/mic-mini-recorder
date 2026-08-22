package com.example.audio

import android.media.AudioDeviceInfo

object RoutePolicy {
    /**
     * Only input-capable Bluetooth routes are valid recording sources.
     * A2DP and BLE speakers are output routes and must never pass this gate.
     */
    fun isExternalBluetooth(type: Int): Boolean =
        type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            type == AudioDeviceInfo.TYPE_BLE_HEADSET

    fun getDeviceTypeName(type: Int): String = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "蓝牙 SCO (DJI Mic)"
        AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE 蓝牙耳机/麦克风"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "蓝牙 A2DP 音频"
        AudioDeviceInfo.TYPE_BUILTIN_MIC -> "手机内置麦克风"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "有线耳机麦克风"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB 音频设备"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB 耳机/麦克风"
        else -> "音频输入 ($type)"
    }
}
