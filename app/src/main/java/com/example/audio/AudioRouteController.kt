package com.example.audio

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioRecord
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.AudioDeviceItem
import com.example.model.AudioRouteInfo

class AudioRouteController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

    private var deviceCallback: AudioDeviceCallback? = null
    var onDevicesChangedListener: (() -> Unit)? = null

    companion object {
        private const val TAG = "AudioRouteController"
    }

    init {
        registerDeviceCallback()
    }

    private fun registerDeviceCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            deviceCallback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    Log.d(TAG, "Audio devices added: ${addedDevices?.size}")
                    onDevicesChangedListener?.invoke()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    Log.d(TAG, "Audio devices removed: ${removedDevices?.size}")
                    onDevicesChangedListener?.invoke()
                }
            }
            audioManager.registerAudioDeviceCallback(deviceCallback, null)
        }
    }

    /**
     * Lists all currently available audio input devices (built-in, bluetooth, usb, wired, etc.)
     * including bonded Bluetooth audio devices.
     */
    fun getAvailableInputDevices(): List<AudioDeviceItem> {
        val list = mutableListOf<AudioDeviceItem>()
        // 0 is always Auto
        list.add(
            AudioDeviceItem(
                id = 0,
                name = "自动智能选择 (优先 DJI / 蓝牙麦克风)",
                type = 0,
                typeName = "智能路由",
                isBluetooth = false,
                isDefault = true
            )
        )

        val seenNames = mutableSetOf<String>()

        try {
            val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            for (dev in inputs) {
                val isBt = RoutePolicy.isExternalBluetooth(dev.type)
                val typeName = RoutePolicy.getDeviceTypeName(dev.type)
                val rawName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    dev.address.takeIf { it.isNotBlank() } ?: dev.productName?.toString()
                } else {
                    dev.productName?.toString()
                }
                val cleanName = if (!rawName.isNullOrBlank() && rawName != "0") {
                    rawName
                } else {
                    typeName
                }

                seenNames.add(cleanName.lowercase())
                list.add(
                    AudioDeviceItem(
                        id = dev.id,
                        name = cleanName,
                        type = dev.type,
                        typeName = typeName,
                        isBluetooth = isBt,
                        isDefault = false
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying audio input devices", e)
        }

        // Also query bonded Bluetooth audio devices that may not currently be streaming
        try {
            val hasBtPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            if (hasBtPerm && bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                val bonded = bluetoothAdapter.bondedDevices
                if (bonded != null) {
                    for (btDev in bonded) {
                        val name = btDev.name ?: "未知蓝牙设备"
                        if (!seenNames.contains(name.lowercase())) {
                            // Use hashcode as a deterministic negative device ID for bonded devices
                            val pseudoId = -Math.abs(btDev.address.hashCode())
                            val isDji = name.contains("DJI", ignoreCase = true) || name.contains("Mic", ignoreCase = true)
                            list.add(
                                AudioDeviceItem(
                                    id = pseudoId,
                                    name = name,
                                    type = AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                                    typeName = if (isDji) "DJI 蓝牙发射器 (已配对)" else "蓝牙音频设备 (已配对)",
                                    isBluetooth = true,
                                    isDefault = false
                                )
                            )
                            seenNames.add(name.lowercase())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error querying bonded bluetooth devices", e)
        }

        return list
    }

    /**
     * Tries to route audio input according to user preference or automatic bluetooth prioritization.
     */
    fun selectAudioDevice(preferredDeviceId: Int): AudioDeviceInfo? {
        try {
            if (preferredDeviceId > 0) {
                val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
                val matched = inputs.firstOrNull { it.id == preferredDeviceId }
                if (matched != null) {
                    val isBt = RoutePolicy.isExternalBluetooth(matched.type)
                    if (isBt) {
                        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val success = audioManager.setCommunicationDevice(matched)
                            Log.d(TAG, "setCommunicationDevice matched device: ${matched.productName}, success=$success")
                        } else {
                            @Suppress("DEPRECATION")
                            audioManager.startBluetoothSco()
                            @Suppress("DEPRECATION")
                            audioManager.isBluetoothScoOn = true
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            audioManager.clearCommunicationDevice()
                        }
                        audioManager.mode = AudioManager.MODE_NORMAL
                    }
                    return matched
                }
            }

            // Fallback / Auto mode: Select Bluetooth if available
            return selectBluetoothCommunicationDevice()
        } catch (e: Exception) {
            Log.e(TAG, "Error selecting audio device $preferredDeviceId", e)
            return null
        }
    }

    /**
     * Tries to route audio input to external Bluetooth device (DJI Mic Mini, BLE Headset, SCO).
     */
    fun selectBluetoothCommunicationDevice(): AudioDeviceInfo? {
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val devices = audioManager.availableCommunicationDevices
                val bluetoothDevice = devices.firstOrNull { RoutePolicy.isExternalBluetooth(it.type) }
                if (bluetoothDevice != null) {
                    val success = audioManager.setCommunicationDevice(bluetoothDevice)
                    Log.d(TAG, "setCommunicationDevice: ${bluetoothDevice.productName}, success=$success")
                    if (success) return bluetoothDevice
                }
                // Check general input devices
                val allInputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
                return allInputs.firstOrNull { RoutePolicy.isExternalBluetooth(it.type) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.startBluetoothSco()
                @Suppress("DEPRECATION")
                audioManager.isBluetoothScoOn = true
                return audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
                    .firstOrNull { RoutePolicy.isExternalBluetooth(it.type) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error selecting bluetooth communication device", e)
            return null
        }
    }

    /**
     * Applies preferred device binding to AudioRecord if supported (API 23+).
     */
    fun applyPreferredDevice(record: AudioRecord, preferredDeviceId: Int): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && preferredDeviceId > 0) {
            try {
                val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
                val target = inputs.firstOrNull { it.id == preferredDeviceId }
                if (target != null) {
                    return record.setPreferredDevice(target)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setPreferredDevice on AudioRecord", e)
            }
        }
        return false
    }

    /**
     * Returns true if the AudioRecord is actually routed through an external Bluetooth device.
     */
    fun hasExternalInput(record: AudioRecord): Boolean {
        val routed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            record.routedDevice
        } else {
            null
        }
        return routed != null && RoutePolicy.isExternalBluetooth(routed.type)
    }

    fun getRouteLabel(record: AudioRecord?): String {
        val routed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            record?.routedDevice
        } else {
            null
        }
        if (routed != null) {
            val name = routed.productName?.toString()?.trim()
            if (!name.isNullOrBlank()) {
                return name
            }
            return RoutePolicy.getDeviceTypeName(routed.type)
        }
        return "默认输入"
    }

    /**
     * Inspects currently available/active audio input routes.
     */
    fun getCurrentRouteInfo(record: AudioRecord? = null, preferredDeviceId: Int = 0): AudioRouteInfo {
        if (record != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val routed = record.routedDevice
            if (routed != null) {
                val isBt = RoutePolicy.isExternalBluetooth(routed.type)
                val name = routed.productName?.toString()?.takeIf { it.isNotBlank() }
                    ?: RoutePolicy.getDeviceTypeName(routed.type)
                return AudioRouteInfo(
                    deviceName = name,
                    deviceType = routed.type,
                    deviceId = routed.id,
                    isExternalBluetooth = isBt,
                    isFallback = !isBt,
                    description = if (isBt) "DJI / 蓝牙麦克风已连接输入" else "当前使用${RoutePolicy.getDeviceTypeName(routed.type)}"
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val commDevice = audioManager.communicationDevice
            if (commDevice != null) {
                val isBt = RoutePolicy.isExternalBluetooth(commDevice.type)
                val name = commDevice.productName?.toString()?.takeIf { it.isNotBlank() }
                    ?: RoutePolicy.getDeviceTypeName(commDevice.type)
                return AudioRouteInfo(
                    deviceName = name,
                    deviceType = commDevice.type,
                    deviceId = commDevice.id,
                    isExternalBluetooth = isBt,
                    isFallback = !isBt,
                    description = if (isBt) "蓝牙通信设备已激活" else "当前通信路由: $name"
                )
            }
        }

        val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        if (preferredDeviceId > 0) {
            val preferred = inputs.firstOrNull { it.id == preferredDeviceId }
            if (preferred != null) {
                val isBt = RoutePolicy.isExternalBluetooth(preferred.type)
                val name = preferred.productName?.toString()?.takeIf { it.isNotBlank() }
                    ?: RoutePolicy.getDeviceTypeName(preferred.type)
                return AudioRouteInfo(
                    deviceName = name,
                    deviceType = preferred.type,
                    deviceId = preferred.id,
                    isExternalBluetooth = isBt,
                    isFallback = !isBt,
                    description = if (isBt) "已指定蓝牙麦克风设备" else "已指定设备: $name"
                )
            }
        }

        val btInput = inputs.firstOrNull { RoutePolicy.isExternalBluetooth(it.type) }
        if (btInput != null) {
            val name = btInput.productName?.toString()?.takeIf { it.isNotBlank() } ?: "DJI 蓝牙发射器"
            return AudioRouteInfo(
                deviceName = name,
                deviceType = btInput.type,
                deviceId = btInput.id,
                isExternalBluetooth = true,
                isFallback = false,
                description = "检测到蓝牙输入设备已配对"
            )
        }

        val builtin = inputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
        if (builtin != null) {
            return AudioRouteInfo(
                deviceName = "手机内置麦克风",
                deviceType = AudioDeviceInfo.TYPE_BUILTIN_MIC,
                deviceId = builtin.id,
                isExternalBluetooth = false,
                isFallback = true,
                description = "未检测到外部蓝牙麦克风"
            )
        }

        return AudioRouteInfo(
            deviceName = "未检测到音频输入",
            deviceType = 0,
            deviceId = 0,
            isExternalBluetooth = false,
            isFallback = true,
            description = "请检查麦克风权限与设备连接"
        )
    }

    fun release() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && deviceCallback != null) {
                audioManager.unregisterAudioDeviceCallback(deviceCallback)
                deviceCallback = null
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            } else {
                @Suppress("DEPRECATION")
                audioManager.stopBluetoothSco()
                @Suppress("DEPRECATION")
                audioManager.isBluetoothScoOn = false
            }
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio routing", e)
        }
    }
}
