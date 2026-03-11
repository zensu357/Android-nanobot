package com.example.nanobot.core.ai

import com.example.nanobot.core.memory.MemorySearchScorer
import com.example.nanobot.core.model.ChatMessage
import com.example.nanobot.core.model.MemoryFact
import com.example.nanobot.core.model.MemorySummary
import com.example.nanobot.core.model.MessageRole
import com.example.nanobot.domain.repository.MemoryRepository
import javax.inject.Inject

class MemoryExposurePlanner @Inject constructor(
    private val memoryRepository: MemoryRepository
) {
    suspend fun buildContext(sessionId: String, latestUserInput: String, recentHistory: List<ChatMessage> = emptyList()): String? {
        return buildWithDiagnostics(sessionId, latestUserInput, recentHistory).context
    }

    suspend fun buildWithDiagnostics(
        sessionId: String,
        latestUserInput: String,
        recentHistory: List<ChatMessage> = emptyList()
    ): MemoryExposureResult {
        val summary = memoryRepository.getSummaryForSession(sessionId)
        val allFacts = memoryRepository.getFacts()
        val scratchEntries = buildScratchEntries(recentHistory, latestUserInput)
        val sessionFacts = selectSessionFacts(sessionId, latestUserInput, allFacts)
        val relevantLongTermFacts = selectLongTermFacts(sessionId, latestUserInput, allFacts)

        if (summary == null && scratchEntries.isEmpty() && sessionFacts.isEmpty() && relevantLongTermFacts.isEmpty()) {
            return MemoryExposureResult(
                context = null,
                summaryIncluded = false,
                scratchEntryCount = 0,
                sessionFactCount = 0,
                longTermFactCount = 0
            )
        }

        val context = buildString {
            appendLine("[Memory]")
            summary?.let {
                appendLine("Session summary:")
                appendLine(formatSummary(it))
            }
            if (scratchEntries.isNotEmpty()) {
                appendLine()
                appendLine("Scratch session memory:")
                scratchEntries.forEach { appendLine("- $it") }
            }
            if (sessionFacts.isNotEmpty()) {
                appendLine()
                appendLine("Relevant current session facts:")
                sessionFacts.forEach { appendLine("- ${formatFact(it)}") }
            }
            if (relevantLongTermFacts.isNotEmpty()) {
                appendLine()
                appendLine("Relevant long-term facts:")
                relevantLongTermFacts.forEach { appendLine("- ${formatFact(it)}") }
            }
        }.trim()
        return MemoryExposureResult(
            context = context,
            summaryIncluded = summary != null,
            scratchEntryCount = scratchEntries.size,
            sessionFactCount = sessionFacts.size,
            longTermFactCount = relevantLongTermFacts.size
        )
    }

    private fun buildScratchEntries(recentHistory: List<ChatMessage>, latestUserInput: String): List<String> {
        val recentUserMessages = recentHistory
            .asReversed()
            .filter { it.role == MessageRole.USER && !it.content.isNullOrBlank() }
            .mapNotNull { it.content?.trim()?.takeIf { value -> value.isNotBlank() } }
            .distinct()
            .take(MAX_SCRATCH_HISTORY)
            .reversed()
        val latest = latestUserInput.trim().takeIf { it.isNotBlank() }
        return buildList {
            latest?.let { add("Latest user request: ${it.take(SCRATCH_ENTRY_MAX_CHARS)}") }
            recentUserMessages.filter { latest == null || it != latest }.forEach { message ->
                add("Recent session context: ${message.take(SCRATCH_ENTRY_MAX_CHARS)}")
            }
        }.take(MAX_SCRATCH_ENTRIES)
    }

    private fun selectSessionFacts(sessionId: String, latestUserInput: String, facts: List<MemoryFact>): List<MemoryFact> {
        val scoped = facts.filter { it.sourceSessionId == sessionId }
        return rankFacts(scoped, latestUserInput, preferredSessionId = sessionId)
            .take(MAX_SESSION_FACTS)
            .ifEmpty { scoped.sortedByDescending { it.updatedAt }.take(FALLBACK_SESSION_FACTS) }
    }

    private fun selectLongTermFacts(sessionId: String, latestUserInput: String, facts: List<MemoryFact>): List<MemoryFact> {
        return rankFacts(
            facts = facts.filter { it.sourceSessionId != sessionId },
            latestUserInput = latestUserInput,
            preferredSessionId = null
        ).take(MAX_LONG_TERM_FACTS)
    }

    private fun rankFacts(
        facts: List<MemoryFact>,
        latestUserInput: String,
        preferredSessionId: String?
    ): List<MemoryFact> {
        val normalizedInput = latestUserInput.trim().lowercase()
        return facts
            .map { fact ->
                fact to MemorySearchScorer.score(
                    query = normalizedInput,
                    text = fact.fact,
                    updatedAt = fact.updatedAt,
                    confidence = fact.confidence,
                    sourceSessionId = fact.sourceSessionId,
                    preferredSessionId = preferredSessionId
                )
            }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .map { (fact, _) -> fact }
    }

    private fun formatSummary(summary: MemorySummary): String {
        val evidence = summary.provenance.evidenceExcerpt?.takeIf { it.isNotBlank() }?.take(80) ?: "none"
        return "${summary.summary} [confidence=${summary.confidence}, evidence=$evidence]"
    }

    private fun formatFact(fact: MemoryFact): String {
        val evidence = fact.provenance.evidenceExcerpt?.takeIf { it.isNotBlank() }?.take(80) ?: "none"
        return "${fact.fact} [confidence=${fact.confidence}, evidence=$evidence]"
    }

    private companion object {
        const val MAX_SCRATCH_ENTRIES = 3
        const val MAX_SCRATCH_HISTORY = 3
        const val SCRATCH_ENTRY_MAX_CHARS = 180
        const val MAX_SESSION_FACTS = 2
        const val FALLBACK_SESSION_FACTS = 1
        const val MAX_LONG_TERM_FACTS = 3
    }
}
