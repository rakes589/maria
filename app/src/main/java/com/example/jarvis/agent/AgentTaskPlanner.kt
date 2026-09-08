package com.example.jarvis.agent

import com.example.jarvis.automation.RootShellHelper
import com.example.jarvis.screen.AccessibilityActionExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface AgentStep { data class Shell(val command: String) : AgentStep; data class ClickText(val text: String) : AgentStep; data object ScrollForward : AgentStep; data class TypeText(val text: String) : AgentStep }
data class AgentPlan(val request: String, val steps: List<AgentStep>)

/** Converts only known, bounded intents into executable steps. Unknown requests go to Gemini. */
object AgentTaskPlanner {
    fun plan(request: String): AgentPlan? {
        val q = request.trim(); if (q.isBlank()) return null
        val steps = mutableListOf<AgentStep>(); val lower = q.lowercase()
        when {
            lower.startsWith("swipe up") -> steps += AgentStep.Shell("input swipe 540 1600 540 500 350")
            lower.startsWith("press back") -> steps += AgentStep.Shell("input keyevent 4")
            lower.startsWith("open settings") -> steps += AgentStep.Shell("am start -a android.settings.SETTINGS")
            lower.startsWith("click ") -> steps += AgentStep.ClickText(q.substringAfter(' ').trim())
            lower.startsWith("type ") -> steps += AgentStep.TypeText(q.substringAfter(' ').trim())
            else -> return null
        }
        return AgentPlan(q, steps)
    }

    suspend fun execute(plan: AgentPlan): Result<String> = withContext(Dispatchers.IO) {
        val outputs = mutableListOf<String>()
        for (step in plan.steps) {
            val ok = when (step) {
                is AgentStep.Shell -> RootShellHelper.execute(step.command).also { outputs += it.diagnostics }.isSuccess
                is AgentStep.ClickText -> AccessibilityActionExecutor.clickText(step.text).also { outputs += "click ${step.text}: $it" }
                AgentStep.ScrollForward -> AccessibilityActionExecutor.scrollForward().also { outputs += "scroll: $it" }
                is AgentStep.TypeText -> AccessibilityActionExecutor.setText(step.text).also { outputs += "type: $it" }
            }
            if (!ok) return@withContext Result.failure(IllegalStateException("Step failed: $step"))
        }
        Result.success(outputs.joinToString("\n"))
    }
}
