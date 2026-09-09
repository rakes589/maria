package com.example.jarvis.automation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.jarvis.agent.AgentState
import com.example.jarvis.agent.AgentStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class VoiceAgentService : Service() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main.immediate + job)
    private val mutex = Mutex()
    private var speech: SpeechHandler? = null
    private var gemini: GeminiAgentManager? = null
    private var tts: TextToSpeechHelper? = null
    private var capsule: FloatingCapsuleController? = null
    private var speaking = false

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("voice_agent", "Voice agent", NotificationManager.IMPORTANCE_LOW))
        startForeground(1001, android.app.Notification.Builder(this, "voice_agent").setContentTitle("Maria active").setContentText("Listening for wake word").setSmallIcon(android.R.drawable.ic_btn_speak_now).build())
        capsule = FloatingCapsuleController(this).also { it.show() }
        scope.launch { AgentStateManager.state.collect { capsule?.update(it) } }
        configureGemini()
        tts = TextToSpeechHelper(this)
        speech = SpeechHandler(this, "maria") { text -> handle(text) }.also { it.start() }
        AgentStateManager.set(AgentState.LISTENING)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RELOAD_API) configureGemini()
        return START_STICKY
    }

    private fun configureGemini() {
        gemini?.close(); gemini = null
        val prefs = getSharedPreferences("agent", MODE_PRIVATE)
        val key = prefs.getString("gemini_key", null).orEmpty()
        val model = prefs.getString("gemini_model", "gemini-2.5-flash").orEmpty().ifBlank { "gemini-2.5-flash" }
        if (key.isNotBlank()) gemini = runCatching { GeminiAgentManager(key, model) }.getOrNull()
    }

    private fun handle(text: String) {
        if (speaking) return
        scope.launch { mutex.withLock {
            speech?.stop(); AgentStateManager.set(AgentState.THINKING)
            val result = gemini?.processTranscript(text)
            if (result?.isSuccess == true) speak(result.getOrThrow()) else speak(if (gemini == null) "Gemini is not configured yet." else "Gemini request failed: ${result?.exceptionOrNull()?.message ?: "unknown error"}")
            if (!job.isCancelled) { speech?.start(); AgentStateManager.set(AgentState.LISTENING) }
        } }
    }

    private suspend fun speak(text: String) { speaking = true; AgentStateManager.set(AgentState.IDLE); try { tts?.speak(text) } finally { speaking = false } }
    override fun onLowMemory() { super.onLowMemory(); speech?.stop(); gemini?.clearTransientCaches(); if (!job.isCancelled && !speaking) speech?.start() }
    override fun onDestroy() { AgentStateManager.reset(); speech?.destroy(); tts?.close(); gemini?.close(); capsule?.close(); capsule = null; scope.cancel(); job.cancel(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object { const val ACTION_RELOAD_API = "com.example.jarvis.action.RELOAD_API" }
}
