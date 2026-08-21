package com.example.micminirecorder

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioRecord
import android.os.Build

class AudioRouteController(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)

    fun selectBluetoothCommunicationDevice(): AudioDeviceInfo? {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val device = audioManager.availableCommunicationDevices
                .firstOrNull { RoutePolicy.isExternalBluetooth(it.type) }
                ?: return null

            return if (audioManager.setCommunicationDevice(device)) device else null
        }

        @Suppress("DEPRECATION")
        audioManager.startBluetoothSco()
        @Suppress("DEPRECATION")
        audioManager.isBluetoothScoOn = true
        return audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            .firstOrNull { RoutePolicy.isExternalBluetooth(it.type) }
    }

    fun hasExternalInput(record: AudioRecord): Boolean {
        val routed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            record.routedDevice
        } else {
            null
        }
        return routed != null && RoutePolicy.isExternalBluetooth(routed.type)
    }

    fun routeLabel(record: AudioRecord): String {
        val routed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            record.routedDevice
        } else {
            null
        }
        return routed?.productName?.toString()?.takeIf { it.isNotBlank() }
            ?: "未知音频设备"
    }

    fun release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        } else {
            @Suppress("DEPRECATION")
            audioManager.stopBluetoothSco()
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn = false
        }
        audioManager.mode = AudioManager.MODE_NORMAL
    }
}
