package com.example.nanobot.core.ai

import com.example.nanobot.core.memory.MemoryFactGovernance
import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.model.ChatMessage
import com.example.nanobot.core.model.LlmChatRequest
import com.example.nanobot.core.model.LlmMessageDto
import com.example.nanobot.core.model.MemoryConsolidationResult
import com.example.nanobot.core.model.MemoryFact
import com.example.nanobot.core.model.MemorySummary
import com.example.nanobot.domain.repository.ChatRepository
import com.example.nanobot.domain.repository.MemoryRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

class MemoryConsolidator @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val chatRepository: ChatRepository,
    private val memoryPromptBuilder: MemoryPromptBuilder
) {
    private val parserJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun shouldConsolidate(
        sessionId: String,
        historySize: Int,
        config: AgentConfig,
        minMessages: Int,
        minNewMessagesDelta: Int
    ): Boolean {
        if (!config.enableMemory) return false
        if (historySize < minMessages) return false

        val existingSummaryCount = getSummarySourceMessageCount(sessionId)
        return existingSummaryCount == null || historySize - existingSummaryCount >= minNewMessagesDelta
    }

    suspend fun consolidate(sessionId: String, history: List<ChatMessage>, config: AgentConfig): Boolean {
        if (!config.enableMemory) return false

        val boundedHistory = history.takeLast(config.memoryWindow.coerceAtLeast(10))
        if (boundedHistory.size < 4) return false

        val existingSummary = memoryRepository.getSummaryForSession(sessionId)
        val existingFacts = memoryRepository.getFacts().take(20)

        val prompt = memoryPromptBuilder.build(
            sessionId = sessionId,
            historyWindow = boundedHistory,
            existingSummary = existingSummary,
            existingFacts = existingFacts
        )

        val responseText = runCatching {
            chatRepository.completeChat(
                request = LlmChatRequest(
                    model = config.model,
                    messages = listOf(
                        LlmMessageDto(
                            role = "system",
                            content = JsonPrimitive(
                                "You are a memory consolidation engine. Return only valid JSON."
                            )
                        ),
                        LlmMessageDto(
                            role = "user",
                            content = JsonPrimitive(prompt)
                        )
                    ),
                    temperature = 0.1,
                    maxTokens = minOf(config.maxTokens, 1200),
                    tools = null,
                    toolChoice = null
                ),
                config = config
            ).content.orEmpty()
        }.getOrElse {
            return false
        }

        val result = parseResult(responseText) ?: return false
        persistResult(
            sessionId = sessionId,
            messageCount = history.size,
            result = result,
            existingFacts = existingFacts
        )
        return true
    }

    suspend fun buildMemoryContext(sessionId: String): String? {
        val summary = memoryRepository.getSummaryForSession(sessionId)?.summary
        val sessionFacts = memoryRepository.getFactsForSession(sessionId).take(3).map { it.fact }
        val longTermFacts = memoryRepository.getFacts()
            .filter { it.sourceSessionId != sessionId }
            .take(5)
            .map { it.fact }

        if (summary.isNullOrBlank() && sessionFacts.isEmpty() && longTermFacts.isEmpty()) {
            return null
        }

        return buildString {
            appendLine("[Memory]")
            if (!summary.isNullOrBlank()) {
                appendLine("Session summary:")
                appendLine(summary)
            }
            if (sessionFacts.isNotEmpty()) {
                appendLine()
                appendLine("Current session facts:")
                sessionFacts.forEach { appendLine("- $it") }
            }
            if (longTermFacts.isNotEmpty()) {
                appendLine()
                appendLine("Long-term user facts:")
                longTermFacts.forEach { appendLine("- $it") }
            }
        }.trim()
    }

    suspend fun getSummarySourceMessageCount(sessionId: String): Int? {
        return memoryRepository.getSummaryForSession(sessionId)?.sourceMessageCount
    }

    private suspend fun persistResult(
        sessionId: String,
        messageCount: Int,
        result: MemoryConsolidationResult,
        existingFacts: List<MemoryFact>
    ) {
        if (result.updatedSummary.isNotBlank()) {
            memoryRepository.upsertSummary(
                MemorySummary(
                    sessionId = sessionId,
                    summary = result.updatedSummary.trim(),
                    updatedAt = System.currentTimeMillis(),
                    sourceMessageCount = messageCount
                )
            )
        }

        val now = System.currentTimeMillis()
        val mutableFacts = existingFacts.toMutableList()
        val existingByNormalized = existingFacts.associateBy { normalizeFact(it.fact) }.toMutableMap()
        result.candidateFacts
            .map { it.trim() }
            .filter { it.length >= 8 }
            .forEach { fact ->
                val normalized = normalizeFact(fact)
                if (normalized.isBlank()) return@forEach

                val existing = existingByNormalized[normalized]
                    ?: MemoryFactGovernance.findReplacementCandidate(mutableFacts, fact)
                if (existing != null) {
                    val updatedFact = existing.copy(
                        fact = fact.take(220),
                        sourceSessionId = sessionId,
                        updatedAt = now
                    )
                    mutableFacts.removeAll { it.id == existing.id }
                    mutableFacts += updatedFact
                    existingByNormalized.remove(normalizeFact(existing.fact))
                    existingByNormalized[normalized] = updatedFact
                    memoryRepository.upsertFact(updatedFact)
                } else {
                    val newFact = MemoryFact(
                        id = UUID.randomUUID().toString(),
                        fact = fact.take(220),
                        sourceSessionId = sessionId,
                        createdAt = now,
                        updatedAt = now
                    )
                    mutableFacts += newFact
                    existingByNormalized[normalized] = newFact
                    memoryRepository.upsertFact(
                        newFact
                    )
                }
            }
        memoryRepository.pruneFacts(MAX_MEMORY_FACTS)
    }

    private fun parseResult(raw: String): MemoryConsolidationResult? {
        return runCatching {
            parserJson.decodeFromString<MemoryConsolidationResult>(raw.trim())
        }.getOrNull()
    }

    private fun normalizeFact(value: String): String = value.trim().lowercase()

    private companion object {
        const val MAX_MEMORY_FACTS = 200
    }
}
