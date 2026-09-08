package com.example.jarvis.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat

class DeviceDiagnostics(private val context: Context) {
    fun microphoneGranted() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    fun notificationsGranted() = android.os.Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    fun overlayGranted() = Settings.canDrawOverlays(context)
    fun accessibilityEnabled(): Boolean = runCatching {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty().contains(context.packageName, ignoreCase = true)
    }.getOrDefault(false)
    fun rootAvailable() = false // RootShellHelper performs the suspend check; this remains a non-blocking UI indicator.
}
