package com.example.jarvis.automation

import com.example.jarvis.screen.ScreenContextService
import com.google.genai.kotlin.Chat
import com.google.genai.kotlin.Client
import com.google.genai.kotlin.types.Blob
import com.google.genai.kotlin.types.Content
import com.google.genai.kotlin.types.FunctionCall
import com.google.genai.kotlin.types.FunctionDeclaration
import com.google.genai.kotlin.types.FunctionResponse
import com.google.genai.kotlin.types.GenerateContentConfig
import com.google.genai.kotlin.types.Part
import com.google.genai.kotlin.types.Schema
import com.google.genai.kotlin.types.Tool
import com.google.genai.kotlin.types.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

class GeminiAgentManager(private val apiKey: String, model: String = "gemini-2.5-flash") : AutoCloseable {
    sealed interface State { data object Idle : State; data object Thinking : State; data object ExecutingTool : State; data class Ready(val text: String) : State; data class Failed(val message: String) : State }
    // Do not initialize the network client during Activity startup when the user has not yet
    // configured credentials. The Builder UI must remain usable without Gemini configured.
    private val client: Client? = apiKey.trim().takeIf { it.isNotEmpty() }?.let { Client(apiKey = it) }
    private val mutex = Mutex()
    private val chat: Chat?
    private val _state = kotlinx.coroutines.flow.MutableStateFlow<State>(State.Idle)
    val state = _state.asStateFlow()
    init { chat = client?.chats?.create(model = model, config = GenerateContentConfig(systemInstruction = Content.fromText("You are Jarvis. Be concise. Use tools only when needed. Never claim an action succeeded unless the tool result says so."), tools = listOf(deviceTool()), temperature = 0.2)) }

    suspend fun processTranscript(text: String): Result<String> = withContext(Dispatchers.IO) { mutex.withLock {
        try { val activeChat = chat ?: error("Gemini API key is not configured"); _state.value = State.Thinking; var response = activeChat.sendMessage(text); var rounds = 5
            while (response.functionCalls.orEmpty().isNotEmpty() && rounds-- > 0) {
                val modelContent = response.candidates?.firstOrNull()?.content ?: error("Missing model function content")
                val call = response.functionCalls!!.singleOrNull()
                response = if (call?.name == "analyzeScreenContext") analyzeScreen(call, modelContent) else {
                    _state.value = State.ExecutingTool
                    val parts = response.functionCalls.orEmpty().map { c -> Part(functionResponse = FunctionResponse(id = c.id, name = c.name, response = dispatch(c))) }
                    activeChat.sendMessage(listOf(modelContent, Content(role = "user", parts = parts)))
                }
            }
            if (response.functionCalls.orEmpty().isNotEmpty()) error("Tool call limit reached")
            val finalText = response.text?.trim()?.takeIf { it.isNotBlank() } ?: error("Empty Gemini response")
            _state.value = State.Ready(finalText)
            Result.success(finalText)
        } catch (t: Throwable) { _state.value = State.Failed(t.message ?: "Gemini failed"); Result.failure<String>(t) }
    } }

    private suspend fun analyzeScreen(call: FunctionCall, modelContent: Content) = withContext(Dispatchers.IO) {
        val capture = RootShellHelper.captureScreenPng().getOrElse { throw it }
        val question = call.args?.get("question")?.jsonPrimitive?.content ?: "What is on screen?"
        val content = Content(role = "user", parts = listOf(
            Part(functionResponse = FunctionResponse(id = call.id, name = call.name, response = mapOf("success" to JsonPrimitive(true)))),
            Part(text = "Question: $question\nAccessibility tree:\n${ScreenContextService.latestSnapshot()}"),
            Part(inlineData = Blob(data = capture.bytes, mimeType = capture.mimeType))
        ))
        (chat ?: error("Gemini API key is not configured")).sendMessage(listOf(modelContent, content))
    }

    private suspend fun dispatch(call: FunctionCall): Map<String, JsonElement> {
        fun arg(name: String) = call.args?.get(name)?.jsonPrimitive?.content?.trim()
        return when (call.name) {
            "executeSystemCommand" -> arg("command")?.let { RootShellHelper.execute(it) }?.let { mapOf("success" to JsonPrimitive(it.isSuccess), "output" to JsonPrimitive(it.diagnostics)) } ?: errorMap("Missing command")
            "adjustDeviceSettings" -> { val s = arg("setting"); val v = arg("value"); if (s == "brightness" && v?.toIntOrNull() in 0..255) { val r = RootShellHelper.execute("settings put system screen_brightness $v"); mapOf("success" to JsonPrimitive(r.isSuccess), "output" to JsonPrimitive(r.diagnostics)) } else errorMap("Unsupported setting") }
            "generateCodeProject" -> errorMap("IDE generation is handled by BuilderViewModel")
            else -> errorMap("Unknown function ${call.name}")
        }
    }
    private fun errorMap(message: String) = mapOf("success" to JsonPrimitive(false), "error" to JsonPrimitive(message))
    private fun objectSchema(vararg fields: Pair<String, Schema>) = Schema(type = Type.OBJECT, properties = fields.toMap(), required = fields.map { it.first })
    private fun deviceTool() = Tool(functionDeclarations = listOf(
        FunctionDeclaration(name = "executeSystemCommand", description = "Execute an approved root shell command.", parameters = objectSchema("command" to Schema(type = Type.STRING))),
        FunctionDeclaration(name = "adjustDeviceSettings", description = "Adjust brightness or volume.", parameters = objectSchema("setting" to Schema(type = Type.STRING), "value" to Schema(type = Type.STRING))),
        FunctionDeclaration(name = "generateCodeProject", description = "Request IDE builder generation.", parameters = objectSchema("prompt" to Schema(type = Type.STRING), "type" to Schema(type = Type.STRING))),
        FunctionDeclaration(name = "analyzeScreenContext", description = "Inspect the current screen using accessibility text and a screenshot.", parameters = objectSchema("question" to Schema(type = Type.STRING)))
    ))
    override fun close() { client?.close() }
    fun clearTransientCaches() = Unit
}
