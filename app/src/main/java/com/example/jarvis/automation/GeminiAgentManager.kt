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
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(modelName.trim().ifBlank { "gemini-2.5-flash" })
    }

    suspend fun processTranscript(text: String): Result<String> = withContext(Dispatchers.IO) { mutex.withLock {
        runCatching {
            require(text.isNotBlank()) { "Empty request" }
            check(FirebaseApp.getApps().isNotEmpty()) { "Firebase is not configured. Add google-services.json and enable Firebase AI Logic." }
            val response = model.generateContent("You are Maria, an Android automation assistant. Answer concisely. User request: $text")
            response.text?.trim()?.takeIf { it.isNotBlank() } ?: error("Gemini returned an empty response")
        }
    } }

    suspend fun testConnection(): Result<String> = processTranscript("Reply with exactly CONNECTION_OK")
    fun clearTransientCaches() = Unit
    override fun close() = Unit
}
