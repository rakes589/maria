package com.example.jarvis.automation

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class TextToSpeechHelper(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ready = kotlinx.coroutines.CompletableDeferred<Unit>()
    @Volatile private var closed = false
    init { Handler(Looper.getMainLooper()).post { tts = TextToSpeech(appContext) { status -> if (status == TextToSpeech.SUCCESS) { tts?.language = Locale.getDefault(); ready.complete(Unit) } else ready.completeExceptionally(IllegalStateException("TTS initialization failed")) } } }
    suspend fun speak(text: String): Result<Unit> = withContext(Dispatchers.Main.immediate) {
        if (text.isBlank()) return@withContext Result.success(Unit)
        try { ready.await(); val engine = tts ?: error("TTS unavailable"); val id = UUID.randomUUID().toString(); suspendCancellableCoroutine { c ->
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() { override fun onStart(id: String?) = Unit; override fun onDone(id: String?) { if (c.isActive) c.resume(Unit) }; override fun onError(id: String?) { if (c.isActive) c.resumeWithException(IllegalStateException("TTS failed")) }; override fun onError(id: String?, code: Int) { if (c.isActive) c.resumeWithException(IllegalStateException("TTS failed: $code")) } })
            c.invokeOnCancellation { engine.stop() }; val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), id); if (result == TextToSpeech.ERROR && c.isActive) c.resumeWithException(IllegalStateException("TTS speak failed"))
        }; Result.success(Unit) } catch (t: Throwable) { Result.failure(t) }
    }
    override fun close() { closed = true; tts?.stop(); tts?.shutdown(); tts = null }
}
