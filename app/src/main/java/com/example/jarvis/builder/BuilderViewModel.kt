package com.example.jarvis.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvis.automation.GeminiAgentManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun extractCodeFromMarkdown(response: String): String { val text = response.replace("\r\n", "\n").trim(); val match = Regex("(?s)```(?:[\\w.+-]+)?\\s*\\n?(.*?)```").find(text); return (match?.groupValues?.get(1) ?: text).trim().removePrefix("html\n").removePrefix("css\n").trim() }
data class BuilderUiState(val files: Map<String, String> = emptyMap(), val selectedFile: String = "index.html", val isLoading: Boolean = true, val errorMessage: String? = null, val previewVersion: Int = 0)

class BuilderViewModel(private val fileManager: ProjectFileManager, private val gemini: GeminiAgentManager) : ViewModel() {
    private val _state = MutableStateFlow(BuilderUiState()); val state = _state.asStateFlow()
    init { reload() }
    fun reload() { viewModelScope.launch { runCatching { withContext(Dispatchers.IO) { fileManager.ensureWorkspace(); fileManager.readAllFiles().getOrThrow() } }.onSuccess { f -> _state.update { it.copy(files = f, isLoading = false, errorMessage = null) } }.onFailure { e -> _state.update { it.copy(isLoading = false, errorMessage = e.message) } } } }
    fun select(file: String) { _state.update { it.copy(selectedFile = file) } }
    fun save(file: String, code: String) { _state.update { it.copy(files = it.files + (file to code)) }; viewModelScope.launch { fileManager.writeFile(file, code).onSuccess { _state.update { it.copy(previewVersion = it.previewVersion + 1) } } } }
    fun executeAutoFix() { if (_state.value.isLoading) return; viewModelScope.launch { _state.update { it.copy(isLoading = true, errorMessage = null) }; try { val current = withContext(Dispatchers.IO) { fileManager.readAllFiles().getOrThrow() }; val fixed = current.toMutableMap(); listOf("index.html", "style.css", "script.js").filter { current.containsKey(it) }.forEach { file -> val prompt = "Act as an expert web developer. Fix bugs and improve the UI in $file. Return ONLY the complete raw code, no Markdown.\n$file:\n${current.getValue(file)}"; fixed[file] = extractCodeFromMarkdown(gemini.processTranscript(prompt).getOrThrow()) }; withContext(Dispatchers.IO) { fixed.forEach { (f, c) -> fileManager.writeFile(f, c).getOrThrow() } }; _state.update { it.copy(files = fixed, isLoading = false, previewVersion = it.previewVersion + 1) } } catch (e: Throwable) { _state.update { it.copy(isLoading = false, errorMessage = e.message) } } } }
    override fun onCleared() { gemini.close(); super.onCleared() }
}
