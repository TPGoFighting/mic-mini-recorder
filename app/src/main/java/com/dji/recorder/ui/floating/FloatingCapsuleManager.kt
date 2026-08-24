package com.dji.recorder.ui.floating

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.dji.recorder.MainActivity
import com.dji.recorder.R
import java.util.Locale

/**
 * 专为 DJI Recorder 打造的「自研流体云悬浮胶囊」（免 OPPO 官方审核，全局置顶显示）。
 * 具备以下特性：
 * 1. 拟态 ColorOS 流体云 / 灵动岛药丸设计（磨砂黑底色 + 动态红点 + 实时走秒）。
 * 2. 支持手指任意拖拽吸附。
 * 3. 点击胶囊可平滑展开「一键停止录音」与「快速返回 App」快捷面板。
 * 4. 录音停止时自动收起并平滑销毁。
 */
object FloatingCapsuleManager {

    private const val TAG = "FloatingCapsuleManager"
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isExpanded = false
    private var isShowing = false

    private var startTimeMs: Long = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var onStopRecordingCallback: (() -> Unit)? = null

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isShowing && startTimeMs > 0) {
                val elapsedSec = (System.currentTimeMillis() - startTimeMs) / 1000
                val timeStr = String.format(Locale.getDefault(), "%02d:%02d", elapsedSec / 60, elapsedSec % 60)
                updateTimerText(timeStr)
                handler.postDelayed(this, 1000)
            }
        }
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun requestOverlayPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show(context: Context, onStopRecording: () -> Unit) {
        if (!hasOverlayPermission(context)) {
            Log.w(TAG, "Cannot show floating capsule: Overlay permission not granted")
            return
        }

        if (isShowing) {
            updateStartTime(System.currentTimeMillis())
            return
        }

        this.onStopRecordingCallback = onStopRecording
        this.startTimeMs = System.currentTimeMillis()

        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                x = 0
                y = 80 // 屏幕顶部状态栏下方
            }

            val view = buildCapsuleView(context, params)
            floatingView = view
            windowManager?.addView(view, params)
            isShowing = true
            handler.post(timerRunnable)

            Log.i(TAG, "Floating capsule displayed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying floating capsule", e)
        }
    }

    fun hide() {
        if (!isShowing) return
        handler.removeCallbacks(timerRunnable)
        try {
            floatingView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing floating view", e)
        } finally {
            floatingView = null
            isShowing = false
            isExpanded = false
            startTimeMs = 0L
            Log.i(TAG, "Floating capsule dismissed")
        }
    }

    fun updateStartTime(newStartTime: Long) {
        this.startTimeMs = newStartTime
    }

    private fun updateTimerText(timeStr: String) {
        floatingView?.findViewById<TextView>(R.id.tv_capsule_timer)?.text = timeStr
        floatingView?.findViewById<TextView>(R.id.tv_expanded_timer)?.text = timeStr
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildCapsuleView(context: Context, params: WindowManager.LayoutParams): View {
        val root = FrameLayout(context)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = context.getDrawable(R.drawable.capsule_bg)
            setPadding(dp2px(context, 14f), dp2px(context, 8f), dp2px(context, 14f), dp2px(context, 8f))
        }

        // --- 1. 紧凑胶囊视图 (Collapsed Capsule) ---
        val collapsedLayout = LinearLayout(context).apply {
            id = R.id.capsule_collapsed_layout
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // 呼吸红点
        val redDot = View(context).apply {
            val size = dp2px(context, 8f)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                rightMargin = dp2px(context, 8f)
            }
            background = context.getDrawable(R.drawable.capsule_red_dot)
        }

        // DJI 标识图标
        val micIcon = ImageView(context).apply {
            setImageResource(R.drawable.dji_mic)
            val size = dp2px(context, 16f)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                rightMargin = dp2px(context, 6f)
            }
        }

        // 走秒文字
        val timerText = TextView(context).apply {
            id = R.id.tv_capsule_timer
            text = "00:00"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 13f
            typeface = android.graphics.Typeface.MONOSPACE
        }

        collapsedLayout.addView(redDot)
        collapsedLayout.addView(micIcon)
        collapsedLayout.addView(timerText)

        // --- 2. 展开快捷控制视图 (Expanded Panel) ---
        val expandedLayout = LinearLayout(context).apply {
            id = R.id.capsule_expanded_layout
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, dp2px(context, 8f), 0, 0)
        }

        val expandedTimer = TextView(context).apply {
            id = R.id.tv_expanded_timer
            text = "00:00"
            setTextColor(0xFFE53935.toInt())
            textSize = 15f
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp2px(context, 8f), 0, 0)
        }

        // 停止录音按钮
        val btnStop = TextView(context).apply {
            text = "🛑 停止"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 12f
            setPadding(dp2px(context, 12f), dp2px(context, 6f), dp2px(context, 12f), dp2px(context, 6f))
            background = context.getDrawable(R.drawable.capsule_btn_red)
            setOnClickListener {
                onStopRecordingCallback?.invoke()
                hide()
            }
        }

        // 打开 App 按钮
        val btnOpenApp = TextView(context).apply {
            text = "↗️ 打开"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 12f
            val paramsMargin = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = dp2px(context, 8f) }
            layoutParams = paramsMargin
            setPadding(dp2px(context, 12f), dp2px(context, 6f), dp2px(context, 12f), dp2px(context, 6f))
            background = context.getDrawable(R.drawable.capsule_btn_surface)
            setOnClickListener {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(intent)
            }
        }

        btnRow.addView(btnStop)
        btnRow.addView(btnOpenApp)

        expandedLayout.addView(expandedTimer)
        expandedLayout.addView(btnRow)

        container.addView(collapsedLayout)
        container.addView(expandedLayout)
        root.addView(container)

        // 交互：点击展开/折叠 + 拖拽移动
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager?.updateViewLayout(root, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // 点击事件：切换展开/折叠
                        isExpanded = !isExpanded
                        expandedLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
                        windowManager?.updateViewLayout(root, params)
                    }
                    true
                }
                else -> false
            }
        }

        return root
    }

    private fun dp2px(context: Context, dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}
