package com.example.jarvis.screen

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Executes bounded, user-authorized accessibility actions against the active window. */
object AccessibilityActionExecutor {
    @Volatile private var service: AccessibilityService? = null
    fun attach(value: AccessibilityService) { service = value }
    fun detach(value: AccessibilityService) { if (service === value) service = null }

    suspend fun clickText(text: String): Boolean = withContext(Dispatchers.Main.immediate) { findText(text)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true }
    suspend fun setText(text: String): Boolean = withContext(Dispatchers.Main.immediate) { val node = findEditable() ?: return@withContext false; val args = android.os.Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }; node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args) }
    suspend fun scrollForward(): Boolean = withContext(Dispatchers.Main.immediate) { service?.rootInActiveWindow?.let { findScrollable(it)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) } == true }
    suspend fun scrollBackward(): Boolean = withContext(Dispatchers.Main.immediate) { service?.rootInActiveWindow?.let { findScrollable(it)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) } == true }
    suspend fun launchPackage(packageName: String): Boolean = withContext(Dispatchers.Main.immediate) { val ctx = service ?: return@withContext false; val intent = ctx.packageManager.getLaunchIntentForPackage(packageName) ?: return@withContext false; intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(intent); true }

    private fun findText(text: String): AccessibilityNodeInfo? { val root = service?.rootInActiveWindow ?: return null; return root.findAccessibilityNodeInfosByText(text).firstOrNull { it.isVisibleToUser } ?: traverse(root) { it.text?.toString()?.contains(text, true) == true || it.contentDescription?.toString()?.contains(text, true) == true } }
    private fun findEditable(): AccessibilityNodeInfo? = service?.rootInActiveWindow?.let { traverse(it) { node -> node.isEditable && node.isVisibleToUser } }
    private fun findScrollable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? = traverse(root) { it.isScrollable && it.isVisibleToUser }
    private fun <T> traverse(root: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? { val queue = ArrayDeque<AccessibilityNodeInfo>(); queue.add(root); var count = 0; while (queue.isNotEmpty() && count++ < 300) { val node = queue.removeFirst(); if (predicate(node)) return node; for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add) }; return null }
}
