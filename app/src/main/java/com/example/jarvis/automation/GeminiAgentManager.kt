package com.example.jarvis.automation

import com.google.firebase.FirebaseApp
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Android-safe Gemini client. Firebase AI Logic keeps provider credentials out of the APK. */
class GeminiAgentManager(private val apiKeyIgnored: String = "", private val modelName: String = "gemini-2.5-flash") : AutoCloseable {
    private val mutex = Mutex()
    private val model by lazy {
        val requested = modelName.trim()
        val safeModel = if (requested.isBlank() || requested == "gemini-flash-latest" || requested == "gemini-1.5-flash") "gemini-2.5-flash" else requested
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(safeModel)
    }

    suspend fun processTranscript(text: String): Result<String> = withContext(Dispatchers.IO) { mutex.withLock {
        runCatching {
            require(text.isNotBlank()) { "Empty request" }
            FirebaseApp.getInstance()
            val response = model.generateContent("You are Maria, an Android automation assistant. Answer concisely. User request: $text")
            response.text?.trim()?.takeIf { it.isNotBlank() } ?: error("Gemini returned an empty response")
        }.recoverCatching { error ->
            val raw = error.message.orEmpty()
            if (raw.contains("JsonDecodingException") || raw.contains("<h1>Bad Request", ignoreCase = true)) {
                error("Firebase returned HTTP 400. Enable Firebase AI Logic/Gemini Developer API, register this app in App Check, and use model gemini-2.5-flash.")
            } else throw error
        }
    } }

    suspend fun testConnection(): Result<String> = processTranscript("Reply with exactly CONNECTION_OK")
    fun clearTransientCaches() = Unit
    override fun close() = Unit
}
