package com.example.jarvis.automation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeechHandler(context: Context, private val wakeWord: String = "maria", private val onCommand: (String) -> Unit) {
    sealed interface State { data object Idle : State; data object Listening : State; data object AwaitingCommand : State; data object Processing : State; data class Error(val message: String) : State }
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()
    private var recognizer: SpeechRecognizer? = null
    private var started = false; private var destroyed = false; private var commandMode = false; private var pending = false
    private val restart = Runnable { pending = false; if (started && !destroyed) listen() }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { set(if (commandMode) State.AwaitingCommand else State.Listening) }
        override fun onBeginningOfSpeech() = Unit; override fun onRmsChanged(rmsdB: Float) = Unit; override fun onBufferReceived(buffer: ByteArray?) = Unit; override fun onEndOfSpeech() = Unit; override fun onPartialResults(results: Bundle?) = Unit; override fun onEvent(eventType: Int, params: Bundle?) = Unit
        override fun onError(error: Int) { if (!destroyed && started) { set(State.Error("Speech error $error")); retry() } }
        override fun onResults(results: Bundle?) {
            if (destroyed || !started) return
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
            if (text.isBlank()) return retry()
            if (commandMode) { commandMode = false; set(State.Processing); onCommand(text); return retry() }
            val index = text.lowercase(Locale.ROOT).indexOf(wakeWord.lowercase(Locale.ROOT))
            if (index < 0) return retry()
            val command = text.substring(index + wakeWord.length).trim()
            if (command.isBlank()) { commandMode = true; set(State.AwaitingCommand); retry(250L) }
            else { set(State.Processing); onCommand(command); retry() }
        }
    }

    fun start() { handler.post { if (!destroyed && !started) { started = true; ensure(); listen() } } }
    fun stop() { handler.post { started = false; pending = false; handler.removeCallbacks(restart); recognizer?.cancel(); set(State.Idle) } }
    fun destroy() { handler.post { if (!destroyed) { destroyed = true; started = false; handler.removeCallbacks(restart); recognizer?.setRecognitionListener(null); recognizer?.cancel(); recognizer?.destroy(); recognizer = null; set(State.Idle) } } }
    private fun ensure() { if (recognizer == null && SpeechRecognizer.isRecognitionAvailable(appContext)) recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply { setRecognitionListener(listener) } }
    private fun listen() { ensure(); val r = recognizer ?: return set(State.Error("Speech recognition unavailable")); try { r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()); putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3) }) } catch (t: Throwable) { set(State.Error(t.message ?: "Unable to listen")); retry() } }
    private fun retry(delay: Long = 400L) { if (!started || destroyed || pending) return; pending = true; recognizer?.cancel(); handler.postDelayed(restart, delay) }
    private fun set(value: State) { if (!destroyed) _state.value = value }
}
