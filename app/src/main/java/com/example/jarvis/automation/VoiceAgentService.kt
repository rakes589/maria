package com.example.jarvis.automation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.jarvis.agent.AgentState
import com.example.jarvis.agent.AgentStateManager
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class VoiceAgentService : Service() {
    private val job = SupervisorJob(); private val scope = CoroutineScope(Dispatchers.Main.immediate + job); private val mutex = Mutex(); private var speech: SpeechHandler? = null; private var gemini: GeminiAgentManager? = null; private var tts: TextToSpeechHelper? = null; private var speaking = false
    override fun onCreate() { super.onCreate(); getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("voice_agent", "Voice agent", NotificationManager.IMPORTANCE_LOW)); startForeground(1001, android.app.Notification.Builder(this, "voice_agent").setContentTitle("Jarvis active").setContentText("Listening for wake word").setSmallIcon(android.R.drawable.ic_btn_speak_now).build()); val key = getSharedPreferences("agent", MODE_PRIVATE).getString("gemini_key", null) ?: return; gemini = GeminiAgentManager(key); tts = TextToSpeechHelper(this); speech = SpeechHandler(this, "maria") { text -> handle(text) }.also { it.start() }; AgentStateManager.set(AgentState.LISTENING) }
    private fun handle(text: String) { if (speaking) return; scope.launch { mutex.withLock { speech?.stop(); AgentStateManager.set(AgentState.THINKING); val result = gemini?.processTranscript(text); if (result?.isSuccess == true) speak(result.getOrThrow()) else speak("I lost connection to the server."); if (!job.isCancelled) { speech?.start(); AgentStateManager.set(AgentState.LISTENING) } } } }
    private suspend fun speak(text: String) { speaking = true; try { tts?.speak(text) } finally { speaking = false } }
    override fun onLowMemory() { super.onLowMemory(); speech?.stop(); gemini?.clearTransientCaches(); scope.launch { if (!job.isCancelled && !speaking) { speech?.start(); AgentStateManager.set(AgentState.LISTENING) } } }
    override fun onDestroy() { AgentStateManager.reset(); speech?.destroy(); tts?.close(); gemini?.close(); scope.cancel(); job.cancel(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
