package com.example.nanobot.core.ai

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
        val facts = memoryRepository.getFacts().take(5).map { it.fact }

        if (summary.isNullOrBlank() && facts.isEmpty()) {
            return null
        }

        return buildString {
            appendLine("[Memory]")
            if (!summary.isNullOrBlank()) {
                appendLine("Session summary:")
                appendLine(summary)
            }
            if (facts.isNotEmpty()) {
                appendLine()
                appendLine("User facts:")
                facts.forEach { appendLine("- $it") }
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

        val existingNormalized = existingFacts.map { normalizeFact(it.fact) }.toMutableSet()
        result.candidateFacts
            .map { it.trim() }
            .filter { it.length >= 8 }
            .forEach { fact ->
                val normalized = normalizeFact(fact)
                if (normalized.isNotBlank() && normalized !in existingNormalized) {
                    existingNormalized += normalized
                    memoryRepository.upsertFact(
                        MemoryFact(
                            id = UUID.randomUUID().toString(),
                            fact = fact.take(220),
                            sourceSessionId = sessionId,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
    }

    private fun parseResult(raw: String): MemoryConsolidationResult? {
        return runCatching {
            parserJson.decodeFromString<MemoryConsolidationResult>(raw.trim())
        }.getOrNull()
    }

    private fun normalizeFact(value: String): String = value.trim().lowercase()
}
