package com.dji.recorder.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.UUID

/**
 * 大疆麦克风全量状态与硬件事件综合捕获器
 */
class DjiHardwareButtonManager(
    private val context: Context,
    private val onButtonClicked: () -> Unit
) {
    companion object {
        private const val TAG = "DjiButtonManager"
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private var isRunning = false
    private var lastTriggerTime = 0L

    // 全方位监听 Android 系统接收到的大疆蓝牙硬件状态跃迁
    private val allEventsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            Log.i(TAG, "⚡ [BROADCAST] Action: $action, extras: ${intent.extras?.keySet()?.joinToString { "$it=${intent.extras?.get(it)}" }}")

            when (action) {
                // 1. 媒体按键 / 快门 / 线控
                Intent.ACTION_MEDIA_BUTTON -> {
                    val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                    }
                    if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                        Log.i(TAG, "🎯 Media Button Triggered: keyCode=${keyEvent.keyCode}")
                        triggerButtonPress("MediaButton Intent [${keyEvent.keyCode}]")
                    }
                }
                // 2. 语音控制键
                Intent.ACTION_VOICE_COMMAND -> {
                    Log.i(TAG, "🎯 Voice Command Triggered")
                    triggerButtonPress("ACTION_VOICE_COMMAND")
                }
                // 3. 蓝牙耳机厂商 AT 指令
                BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT -> {
                    val cmd = intent.getStringExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD)
                    Log.i(TAG, "🎯 Vendor AT Event: $cmd")
                    triggerButtonPress("HFP AT: $cmd")
                }
                // 4. 蓝牙音频路由变化
                BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1)
                    Log.i(TAG, "ℹ️ Headset Audio State Changed: $state")
                }
                AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED -> {
                    val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
                    Log.i(TAG, "ℹ️ SCO Audio State Updated: $state")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (isRunning) return
        isRunning = true
        Log.i(TAG, "Starting All-Channel DJI Hardware Button Listener...")

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_BUTTON)
            addAction(Intent.ACTION_VOICE_COMMAND)
            addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)
            addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY + ".*")
            priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        }

        try {
            context.registerReceiver(allEventsReceiver, filter, Context.RECEIVER_EXPORTED)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register allEventsReceiver", e)
        }
    }

    /**
     * 防抖节流分发物理按键触发事件 (Debounced 600ms)
     */
    fun triggerButtonPress(source: String) {
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < 800) {
            Log.d(TAG, "Ignored trigger in debounce window from $source")
            return
        }
        lastTriggerTime = now
        Log.i(TAG, ">>> SUCCESSFUL BUTTON TRIGGER FROM [$source] <<<")

        CoroutineScope(Dispatchers.Main).launch {
            onButtonClicked()
        }
    }

    fun stopListening() {
        isRunning = false
        try { context.unregisterReceiver(allEventsReceiver) } catch (e: Exception) { /* ignore */ }
    }
}
