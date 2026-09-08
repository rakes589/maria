package com.example.jarvis.builder

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProjectFileManager(context: Context, projectName: String = "landing-page") {
    private val root = File(context.applicationContext.filesDir, "projects/${projectName.replace(Regex("[^A-Za-z0-9._-]"), "_")}")
    val workspaceDir get() = root
    suspend fun ensureWorkspace() = withContext(Dispatchers.IO) { require(root.mkdirs() || root.isDirectory); defaults.forEach { (n, c) -> val f = safeFile(n); if (!f.exists()) f.writeText(c) }; root }
    suspend fun writeFile(path: String, content: String): Result<Unit> = withContext(Dispatchers.IO) { runCatching { val f = safeFile(path); require(f.parentFile?.let { it.isDirectory || it.mkdirs() } != false); val tmp = File(f.parentFile, ".${f.name}.tmp"); tmp.writeText(content); require(tmp.renameTo(f) || f.writeTextAndReturn(content).let { tmp.delete(); true }) } }
    suspend fun readAllFiles(): Result<Map<String, String>> = withContext(Dispatchers.IO) { runCatching { ensureWorkspace(); root.walkTopDown().filter { it.isFile && !it.name.endsWith(".tmp") }.associate { it.relativeTo(root).invariantSeparatorsPath to it.readText() } } }
    suspend fun readFile(path: String): Result<String> = withContext(Dispatchers.IO) { runCatching { ensureWorkspace(); safeFile(path).takeIf { it.isFile }?.readText() ?: "" } }
    fun safeFile(path: String): File { require(path.isNotBlank() && !path.startsWith('/')); val r = root.canonicalFile; val f = File(r, path).canonicalFile; require(f == r || f.path.startsWith(r.path + File.separator)); return f }
    private fun File.writeTextAndReturn(value: String) { writeText(value) }
    private val defaults = mapOf("index.html" to "<!doctype html><html><head><link rel=\"stylesheet\" href=\"style.css\"></head><body><h1>Jarvis Builder</h1><script src=\"script.js\"></script></body></html>", "style.css" to "body{font-family:system-ui;margin:3rem}", "script.js" to "console.log('Jarvis project loaded');")
}
