package com.example.jarvis.builder

data class CodeDiagnostic(val line: Int, val message: String)

object CodeDiagnostics {
    fun analyze(file: String, code: String): List<CodeDiagnostic> {
        val result = mutableListOf<CodeDiagnostic>(); val lines = code.lines()
        if (file.endsWith(".html")) { val opens = Regex("<([A-Za-z][A-Za-z0-9]*)[^>/]*>").findAll(code).map { it.groupValues[1].lowercase() }.toMutableList(); Regex("</([A-Za-z][A-Za-z0-9]*)>").findAll(code).forEach { tag -> if (opens.removeLastOrNull() != tag.groupValues[1].lowercase()) result += CodeDiagnostic(lines.take(code.substring(0, tag.range.first).count { it == '\n' }).size.coerceAtLeast(1), "Unexpected closing tag ${tag.groupValues[1]}") }; if (opens.isNotEmpty()) result += CodeDiagnostic(1, "Unclosed tag ${opens.last()}") }
        val balance = code.count { it == '{' } - code.count { it == '}' }; if (balance != 0) result += CodeDiagnostic(1, "Unbalanced braces (${if (balance > 0) "missing }" else "extra }"})")
        return result.take(20)
    }
}
