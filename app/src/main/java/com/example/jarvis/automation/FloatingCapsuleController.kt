package com.example.jarvis.automation

import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.example.jarvis.agent.AgentState

/** Owns the overlay view and removes it deterministically with the service lifecycle. */
class FloatingCapsuleController(private val service: android.content.Context) : AutoCloseable {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private var container: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var state = AgentState.IDLE
    private var downX = 0f; private var downY = 0f; private var startX = 0; private var startY = 0

    fun show() {
        if (container != null || !android.provider.Settings.canDrawOverlays(service)) return
        val label = TextView(service).apply { setTextColor(Color.WHITE); textSize = 10f; setPadding(18, 9, 18, 9); typeface = android.graphics.Typeface.DEFAULT_BOLD; text = "MARIA • READY" }
        val root = LinearLayout(service).apply { gravity = Gravity.CENTER; addView(label); background = backgroundFor(state); setOnTouchListener { _, event -> onTouch(event); true } }
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        val layout = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; x = 0; y = 48 }
        runCatching { windowManager.addView(root, layout); container = root; params = layout }
    }

    fun update(newState: AgentState) { state = newState; val root = container ?: return; root.background = backgroundFor(newState); (root.getChildAt(0) as? TextView)?.text = "MARIA • ${newState.name}" }
    private fun backgroundFor(value: AgentState) = GradientDrawable().apply { cornerRadius = 60f; setColor(when (value) { AgentState.LISTENING -> Color.rgb(190, 35, 105); AgentState.THINKING -> Color.rgb(210, 105, 40); AgentState.EXECUTING_ROOT -> Color.rgb(100, 60, 190); AgentState.CODING -> Color.rgb(20, 150, 145); AgentState.ERROR -> Color.rgb(180, 35, 45); else -> Color.rgb(25, 40, 55) }); setStroke(1, Color.argb(180, 54, 224, 208)) }
    private fun onTouch(event: MotionEvent): Boolean { val p = params ?: return false; val root = container ?: return false; when (event.actionMasked) { MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; startX = p.x; startY = p.y }; MotionEvent.ACTION_MOVE -> { p.x = startX + (event.rawX - downX).toInt(); p.y = startY + (event.rawY - downY).toInt(); runCatching { windowManager.updateViewLayout(root, p) } } }; return true }
    override fun close() { container?.let { runCatching { windowManager.removeViewImmediate(it) } }; container = null; params = null }
}
