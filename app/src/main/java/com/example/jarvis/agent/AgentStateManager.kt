package com.example.jarvis.agent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AgentState { IDLE, LISTENING, THINKING, EXECUTING_ROOT, CODING, ERROR }

object AgentStateManager {
    private val _state = MutableStateFlow(AgentState.IDLE)
    val state: StateFlow<AgentState> = _state.asStateFlow()
    fun set(value: AgentState) { _state.value = value }
    fun reset() { _state.value = AgentState.IDLE }
}
