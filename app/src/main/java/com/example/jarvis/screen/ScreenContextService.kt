package com.example.jarvis.screen

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicReference

class ScreenContextService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { when (event?.eventType) { AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, AccessibilityEvent.TYPE_VIEW_SCROLLED, AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> latest.set(captureCurrentUiTree()) } }
    override fun onInterrupt() = Unit
    override fun onServiceConnected() { super.onServiceConnected(); AccessibilityActionExecutor.attach(this); latest.set(captureCurrentUiTree()) }
    override fun onDestroy() { AccessibilityActionExecutor.detach(this); super.onDestroy() }
    fun captureCurrentUiTree(): String {
        val root = rootInActiveWindow ?: return "No active window"
        val q = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>(); val out = StringBuilder(); q.add(root to 0); var count = 0
        while (q.isNotEmpty() && count++ < 500) { val (n, depth) = q.removeFirst(); val text = n.text?.toString()?.trim().orEmpty(); val desc = n.contentDescription?.toString()?.trim().orEmpty(); if (text.isNotBlank() || desc.isNotBlank() || n.isClickable) out.append("  ".repeat(depth.coerceAtMost(20))).append(n.className?.toString()?.substringAfterLast('.')).append(if (n.isClickable) " [clickable]" else "").append(" text=").append(text.take(200)).append(" desc=").append(desc.take(200)).append('\n'); if (depth < 20) for (i in 0 until n.childCount) n.getChild(i)?.let { q.add(it to depth + 1) } }
        return out.toString().ifBlank { "No readable UI elements" }
    }
    companion object { private val latest = AtomicReference("No screen context captured"); fun latestSnapshot() = latest.get() }
}
