package com.example.jarvis.automation

import com.topjohnwu.superuser.Shell
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class CommandResult(val exitCode: Int, val stdout: List<String> = emptyList(), val stderr: List<String> = emptyList(), val exceptionMessage: String? = null) {
    val isSuccess get() = exceptionMessage == null && exitCode == 0
    val diagnostics get() = (stdout + stderr + listOfNotNull(exceptionMessage)).joinToString("\n")
}
data class ScreenCapture(val bytes: ByteArray, val mimeType: String = "image/png", val path: String)

object RootShellHelper {
    private val lock = Mutex()
    @Volatile private var shell: Shell? = null

    suspend fun isRootAccessGranted(): Boolean = withContext(Dispatchers.IO) {
        try { getShell()?.isRoot == true } catch (_: Throwable) { false }
    }

    suspend fun execute(command: String): CommandResult = withContext(Dispatchers.IO) {
        lock.withLock {
            try {
                val active = getShell() ?: return@withLock CommandResult(Shell.Result.JOB_NOT_EXECUTED, exceptionMessage = "Root unavailable")
                val out = mutableListOf<String>(); val err = mutableListOf<String>()
                val result = active.newJob().add(command).to(out, err).exec()
                CommandResult(result.code, out.toList(), err.toList())
            } catch (t: Throwable) {
                CommandResult(Shell.Result.JOB_NOT_EXECUTED, exceptionMessage = t.message ?: t.javaClass.simpleName)
            }
        }
    }

    suspend fun captureScreenPng(): Result<ScreenCapture> = withContext(Dispatchers.IO) {
        val path = "/sdcard/agent_screen.png"
        try {
            val result = execute("screencap -p ${quote(path)}")
            if (!result.isSuccess) error("screencap failed: ${result.diagnostics}")
            val file = File(path); require(file.isFile && file.length() > 0) { "Screenshot was not created" }
            Result.success(ScreenCapture(file.readBytes(), path = path))
        } catch (t: Throwable) { Result.failure(t) }
        finally { runCatching { File(path).delete() } }
    }

    fun invalidate() { shell = null }

    private fun getShell(): Shell? {
        val cached = shell
        if (cached != null && cached.isAlive && cached.isRoot) return cached
        synchronized(this) {
            val current = shell
            if (current != null && current.isAlive && current.isRoot) return current
            val created = Shell.getShell()
            return if (created.isAlive && created.isRoot) created.also { shell = it } else null.also { shell = null }
        }
    }

    private fun quote(value: String) = "'${value.replace("'", "'\\''")}'"
}
