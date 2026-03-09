package com.example.nanobot.core.tools.impl

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.model.AgentRunContext
import com.example.nanobot.core.tools.AgentTool
import com.example.nanobot.core.tools.ToolAccessCategory
import com.example.nanobot.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class SessionSnapshotTool @Inject constructor(
    private val sessionRepository: SessionRepository
) : AgentTool {
    override val name: String = "session_snapshot"
    override val description: String = "Returns a compact snapshot of a session and recent messages"
    override val accessCategory: ToolAccessCategory = ToolAccessCategory.LOCAL_READ_ONLY
    override val availabilityHint: String = "Available when session storage is enabled"
    override val parametersSchema: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("sessionId") {
                put("type", "string")
                put("description", "Optional session id; defaults to current session")
            }
        }
    }

    override suspend fun execute(arguments: JsonObject, config: AgentConfig, runContext: AgentRunContext): String {
        val requestedSessionId = arguments["sessionId"]?.jsonPrimitive?.contentOrNull
        val session = if (requestedSessionId.isNullOrBlank()) {
            sessionRepository.observeSessionsSnapshot().firstOrNull { it.id == runContext.sessionId }
                ?: sessionRepository.getOrCreateCurrentSession()
        } else {
            sessionRepository.observeSessionsSnapshot().firstOrNull { it.id == requestedSessionId }
                ?: return "Session '$requestedSessionId' was not found."
        }
        val messages = sessionRepository.getMessages(session.id)
        val preview = messages.takeLast(5).joinToString(separator = "\n") { message ->
            "- ${message.role.name}: ${message.content.orEmpty().take(120)}"
        }
        return buildString {
            appendLine("Session ID: ${session.id}")
            appendLine("Title: ${session.title}")
            appendLine("Message Count: ${messages.size}")
            if (preview.isNotBlank()) {
                appendLine("Recent Messages:")
                appendLine(preview)
            }
        }.trim()
    }
}
