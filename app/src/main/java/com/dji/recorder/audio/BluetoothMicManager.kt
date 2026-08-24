package com.dji.recorder.audio

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.dji.recorder.model.BluetoothMicDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 专为 DJI Mic 及各类蓝牙/外接麦克风打造的现代音频路由管理器。
 * 遵循 Android 官方最佳实践：
 * 1. 进入 MODE_IN_COMMUNICATION 模式。
 * 2. 启用 setCommunicationDevice(device) 绑定外部麦克风。
 * 3. 兼容旧版本自动回退 startBluetoothSco()。
 */
class BluetoothMicManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val TAG = "BluetoothMicManager"

    private val _connectedDevice = MutableStateFlow<BluetoothMicDevice?>(null)
    val connectedDevice: StateFlow<BluetoothMicDevice?> = _connectedDevice.asStateFlow()

    private val _isScoConnected = MutableStateFlow(false)
    val isScoConnected: StateFlow<Boolean> = _isScoConnected.asStateFlow()

    private var onDeviceDisconnectedListener: (() -> Unit)? = null

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            Log.d(TAG, "Audio devices added: ${addedDevices?.size}")
            refreshConnectedBluetoothMics()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            Log.d(TAG, "Audio devices removed: ${removedDevices?.size}")
            val current = _connectedDevice.value
            if (current != null && removedDevices?.any { it.id == current.id } == true) {
                Log.w(TAG, "Active Bluetooth Mic disconnected: ${current.name}")
                _connectedDevice.value = null
                onDeviceDisconnectedListener?.invoke()
            }
            refreshConnectedBluetoothMics()
        }
    }

    private val scoReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val state = intent?.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1) ?: -1
            Log.d(TAG, "SCO audio state broadcast: $state")
            when (state) {
                AudioManager.SCO_AUDIO_STATE_CONNECTED -> _isScoConnected.value = true
                AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> _isScoConnected.value = false
            }
        }
    }

    fun startListening(onDisconnected: () -> Unit) {
        this.onDeviceDisconnectedListener = onDisconnected
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
        
        val filter = IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        context.registerReceiver(scoReceiver, filter)

        refreshConnectedBluetoothMics()
    }

    fun stopListening() {
        try {
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
            context.unregisterReceiver(scoReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering callbacks", e)
        }
        deactivateMicRouting()
    }

    /**
     * 激活外部麦克风输入路由（必须先切换 MODE_IN_COMMUNICATION）
     */
    @SuppressLint("MissingPermission")
    fun activateMicRouting(device: BluetoothMicDevice) {
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val success = audioManager.setCommunicationDevice(device.rawDeviceInfo)
                Log.i(TAG, "setCommunicationDevice (${device.name}): $success")
                if (!success) {
                    audioManager.isBluetoothScoOn = true
                    audioManager.startBluetoothSco()
                }
            } else {
                audioManager.isBluetoothScoOn = true
                audioManager.startBluetoothSco()
                Log.i(TAG, "Legacy startBluetoothSco activated")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error activating mic routing", e)
        }
    }

    /**
     * 释放麦克风输入路由
     */
    fun deactivateMicRouting() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            }
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
            Log.i(TAG, "deactivateMicRouting completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error deactivating mic routing", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshConnectedBluetoothMics(): BluetoothMicDevice? {
        val inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        val btAudioDevice = inputDevices.firstOrNull { isBluetoothOrExternalMic(it) }

        if (btAudioDevice != null) {
            val resolvedName = resolveRealBluetoothName(btAudioDevice)
            val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) btAudioDevice.address else null

            val dev = BluetoothMicDevice(
                id = btAudioDevice.id,
                name = resolvedName,
                address = address,
                type = btAudioDevice.type,
                rawDeviceInfo = btAudioDevice
            )
            _connectedDevice.value = dev
            Log.i(TAG, "External Mic locked: $resolvedName (Type: ${btAudioDevice.type})")
            return dev
        } else {
            Log.d(TAG, "No external mic found in input devices")
            _connectedDevice.value = null
            return null
        }
    }

    @SuppressLint("MissingPermission")
    private fun resolveRealBluetoothName(audioDeviceInfo: AudioDeviceInfo): String {
        val phoneModel = Build.MODEL
        val phoneDevice = Build.DEVICE
        val rawName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            audioDeviceInfo.productName?.toString()
        } else null

        try {
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                val bonded = bluetoothAdapter.bondedDevices
                if (!bonded.isNullOrEmpty()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !audioDeviceInfo.address.isNullOrBlank()) {
                        val matched = bonded.firstOrNull { it.address.equals(audioDeviceInfo.address, ignoreCase = true) }
                        if (matched != null) {
                            val name = getDeviceName(matched)
                            if (!name.isNullOrBlank() && !name.equals(phoneModel, ignoreCase = true)) {
                                return name
                            }
                        }
                    }

                    val djiDevice = bonded.firstOrNull { d ->
                        val n = getDeviceName(d) ?: ""
                        n.contains("dji", ignoreCase = true) ||
                        n.contains("mic", ignoreCase = true) ||
                        n.contains("wireless", ignoreCase = true)
                    }
                    if (djiDevice != null) {
                        return getDeviceName(djiDevice) ?: "DJI Mic"
                    }

                    val audioBtDevice = bonded.firstOrNull { d ->
                        val n = getDeviceName(d) ?: ""
                        !n.equals(phoneModel, ignoreCase = true) &&
                        !n.equals(phoneDevice, ignoreCase = true) &&
                        (d.bluetoothClass?.hasService(BluetoothClass.Service.AUDIO) == true ||
                         d.bluetoothClass?.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET ||
                         d.bluetoothClass?.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES ||
                         d.bluetoothClass?.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE)
                    }
                    if (audioBtDevice != null) {
                        return getDeviceName(audioBtDevice) ?: "Bluetooth Mic"
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve peripheral name", e)
        }

        if (!rawName.isNullOrBlank() &&
            !rawName.equals(phoneModel, ignoreCase = true) &&
            !rawName.equals(phoneDevice, ignoreCase = true)) {
            return rawName
        }

        return "DJI 外部麦克风"
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceName(device: BluetoothDevice): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            device.alias ?: device.name
        } else {
            device.name
        }
    }

    private fun isBluetoothOrExternalMic(device: AudioDeviceInfo): Boolean {
        if (!device.isSource) return false
        return when (device.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> true
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> true
            AudioDeviceInfo.TYPE_BLE_HEADSET -> true
            AudioDeviceInfo.TYPE_USB_DEVICE -> true
            AudioDeviceInfo.TYPE_USB_HEADSET -> true
            26, 30, 31 -> true
            else -> false
        }
    }
}
