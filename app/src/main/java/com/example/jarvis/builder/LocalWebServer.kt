package com.example.jarvis.builder

import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.net.URLConnection

class LocalWebServer(private val files: ProjectFileManager, port: Int = 8080) : NanoHTTPD("127.0.0.1", port) {
    override fun serve(session: IHTTPSession): Response {
        if (session.method != Method.GET && session.method != Method.HEAD) return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain", "Only GET and HEAD")
        return try { val path = session.uri.removePrefix("/").ifBlank { "index.html" }; val file = files.safeFile(path).canonicalFile; if (!file.isFile) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found"); val mime = URLConnection.guessContentTypeFromName(file.name) ?: when (file.extension) { "html" -> "text/html; charset=utf-8"; "css" -> "text/css; charset=utf-8"; "js" -> "application/javascript; charset=utf-8"; else -> "application/octet-stream" }; if (session.method == Method.HEAD) newFixedLengthResponse(Response.Status.OK, mime, "") else newChunkedResponse(Response.Status.OK, mime, FileInputStream(file)) } catch (_: Throwable) { newFixedLengthResponse(Response.Status.FORBIDDEN, "text/plain", "Forbidden") }
    }
}
