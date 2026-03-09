package com.example.nanobot.core.ai

import com.example.nanobot.core.model.ChatMessage
import com.example.nanobot.core.model.MemoryFact
import com.example.nanobot.core.model.MemorySummary
import javax.inject.Inject

class MemoryPromptBuilder @Inject constructor() {
    fun build(
        sessionId: String,
        historyWindow: List<ChatMessage>,
        existingSummary: MemorySummary?,
        existingFacts: List<MemoryFact>
    ): String {
        return buildString {
            appendLine("You are consolidating memory for an Android-native assistant.")
            appendLine("Summarize the session and extract only stable, useful long-term facts.")
            appendLine("Do not include transient requests, one-off tool outputs, or speculative facts.")
            appendLine()
            appendLine("Return valid JSON with this exact shape:")
            appendLine("{\"updatedSummary\":\"...\",\"candidateFacts\":[\"...\",\"...\"]}")
            appendLine()
            appendLine("Session ID: $sessionId")
            appendLine()
            appendLine("Existing summary:")
            appendLine(existingSummary?.summary ?: "(none)")
            appendLine()
            appendLine("Existing long-term facts:")
            if (existingFacts.isEmpty()) {
                appendLine("(none)")
            } else {
                existingFacts.take(10).forEach { appendLine("- ${it.fact}") }
            }
            appendLine()
            appendLine("Recent session history:")
            historyWindow.forEach { message ->
                appendLine("- ${message.role.name}: ${message.content.orEmpty().take(240)}")
            }
            appendLine()
            appendLine("Rules:")
            appendLine("- Keep updatedSummary concise but specific.")
            appendLine("- candidateFacts must contain only stable, reusable facts.")
            appendLine("- If no strong facts exist, return an empty array.")
            appendLine("- Output JSON only. No markdown fences.")
        }.trim()
    }
}
