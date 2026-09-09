package com.example.jarvis.automation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

/** Talks only to Maria Gateway. Provider API keys never enter the Android APK. */
class GeminiAgentManager(
    private val endpoint: String,
    private val gatewayToken: String = "",
    private val provider: String = "auto",
    private val modelName: String = "default"
) : AutoCloseable {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun processTranscript(text: String): Result<String> = withContext(Dispatchers.IO) { mutex.withLock {
        runCatching {
            require(text.isNotBlank()) { "Empty request" }
            val base = endpoint.trim().removeSuffix("/")
            require(base.startsWith("https://") || base.startsWith("http://")) { "Gateway URL must start with https://" }
            val connection = (URL("$base/api/chat").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; connectTimeout = 15_000; readTimeout = 60_000; doOutput = true
                setRequestProperty("Content-Type", "application/json")
                if (gatewayToken.isNotBlank()) setRequestProperty("Authorization", "Bearer $gatewayToken")
            }
            val payload = "{\"message\":${quote(text)},\"provider\":${quote(provider)},\"model\":${quote(modelName)}}"
            connection.outputStream.use { it.write(payload.toByteArray()) }
            val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (connection.responseCode !in 200..299) error("Gateway HTTP ${connection.responseCode}: ${body.take(300)}")
            val data = json.parseToJsonElement(body).jsonObject
            data["text"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                ?: error(data["error"]?.jsonPrimitive?.content ?: "Gateway returned no text")
        }
    } }

    suspend fun testConnection(): Result<String> = processTranscript("Reply with exactly CONNECTION_OK")
    private fun quote(value: String) = Json.encodeToString(kotlinx.serialization.json.JsonPrimitive(value))
    override fun close() = Unit
}
