package com.example.nanobot.core.ai

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.model.HeartbeatDecisionResult
import com.example.nanobot.core.model.LlmChatRequest
import com.example.nanobot.core.model.LlmMessageDto
import com.example.nanobot.domain.repository.ChatRepository
import com.example.nanobot.domain.repository.HeartbeatRepository
import javax.inject.Inject
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

interface HeartbeatDecisionEngine {
    suspend fun decide(config: AgentConfig): HeartbeatDecisionResult
}

class HeartbeatDecider @Inject constructor(
    private val heartbeatRepository: HeartbeatRepository,
    private val chatRepository: ChatRepository
) : HeartbeatDecisionEngine {
    private val parserJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun decide(config: AgentConfig): HeartbeatDecisionResult {
        if (!heartbeatRepository.isHeartbeatEnabled()) {
            return SKIP_RESULT
        }

        val instructions = heartbeatRepository.getHeartbeatInstructions().trim()
        if (instructions.isBlank()) {
            return SKIP_RESULT
        }

        val rawResponse = runCatching {
            chatRepository.completeChat(
                request = LlmChatRequest(
                    model = config.model,
                    messages = listOf(
                        LlmMessageDto(
                            role = "system",
                            content = JsonPrimitive(
                                "You are Nanobot's heartbeat agent. Review the local heartbeat instructions and decide whether there is an active task that should run now. Return only valid JSON with shape {\"action\":\"skip|run\",\"tasks\":\"...\"}. Use action=skip when there is no active task, the instructions are informational only, or execution should wait. Use action=run only when there is a concrete task to execute now."
                            )
                        ),
                        LlmMessageDto(
                            role = "user",
                            content = JsonPrimitive(
                                buildPrompt(instructions)
                            )
                        )
                    ),
                    temperature = 0.1,
                    maxTokens = minOf(config.maxTokens, MAX_DECISION_TOKENS),
                    tools = null,
                    toolChoice = null
                ),
                config = config
            ).content.orEmpty()
        }.getOrNull() ?: return SKIP_RESULT

        return parseDecision(rawResponse)
    }

    private fun buildPrompt(instructions: String): String {
        return buildString {
            appendLine("Current local time (epoch millis): ${System.currentTimeMillis()}")
            appendLine()
            appendLine("Heartbeat instructions:")
            appendLine(instructions)
            appendLine()
            appendLine("Decision rules:")
            appendLine("- Return {\"action\":\"skip\",\"tasks\":\"\"} when there is nothing actionable right now.")
            appendLine("- Return {\"action\":\"run\",\"tasks\":\"clear execution brief\"} when there is an active task to execute now.")
            appendLine("- Keep tasks concise, specific, and ready to send to the main agent.")
        }
    }

    private fun parseDecision(raw: String): HeartbeatDecisionResult {
        val jsonPayload = extractJsonObject(raw) ?: return SKIP_RESULT
        val parsed = runCatching {
            parserJson.decodeFromString<HeartbeatDecisionResult>(jsonPayload)
        }.getOrNull() ?: return SKIP_RESULT

        return when (parsed.action.trim().lowercase()) {
            ACTION_RUN -> {
                val normalizedTasks = parsed.tasks.trim()
                if (normalizedTasks.isBlank()) SKIP_RESULT else HeartbeatDecisionResult(ACTION_RUN, normalizedTasks)
            }
            ACTION_SKIP -> SKIP_RESULT
            else -> SKIP_RESULT
        }
    }

    private fun extractJsonObject(raw: String): String? {
        val trimmed = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) {
            return null
        }
        return trimmed.substring(start, end + 1)
    }

    private companion object {
        const val ACTION_SKIP = "skip"
        const val ACTION_RUN = "run"
        const val MAX_DECISION_TOKENS = 300
        val SKIP_RESULT = HeartbeatDecisionResult(action = ACTION_SKIP, tasks = "")
    }
}
