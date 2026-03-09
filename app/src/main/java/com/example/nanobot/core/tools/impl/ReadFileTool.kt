package com.example.nanobot.core.tools.impl

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.model.AgentRunContext
import com.example.nanobot.core.tools.AgentTool
import com.example.nanobot.core.tools.ToolAccessCategory
import com.example.nanobot.domain.repository.WorkspaceRepository
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class ReadFileTool @Inject constructor(
    private val workspaceRepository: WorkspaceRepository
) : AgentTool {
    override val name: String = "read_file"
    override val description: String = "Reads text from a sandboxed workspace file with strict size limits"
    override val accessCategory: ToolAccessCategory = ToolAccessCategory.WORKSPACE_READ_ONLY
    override val availabilityHint: String = "Read-only access limited to workspace:/"
    override val parametersSchema: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("relativePath") {
                put("type", "string")
                put("description", "Workspace-relative file path to read")
            }
            putJsonObject("maxChars") {
                put("type", "integer")
                put("description", "Optional max number of characters to return; defaults to 4000")
            }
        }
        put("required", buildJsonArray { add(JsonPrimitive("relativePath")) })
    }

    override suspend fun execute(arguments: JsonObject, config: AgentConfig, runContext: AgentRunContext): String {
        val relativePath = arguments["relativePath"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
        val maxChars = arguments["maxChars"]?.jsonPrimitive?.intOrNull ?: 4_000
        if (relativePath.isBlank()) {
            return "The 'relativePath' field is required for read_file."
        }

        return runCatching {
            val result = workspaceRepository.readText(relativePath, maxChars)
            buildString {
                appendLine("Workspace file: ${result.relativePath}")
                appendLine("Bytes: ${result.totalBytes}")
                appendLine("Truncated: ${result.truncated}")
                appendLine("Content:")
                append(result.content)
            }.trim()
        }.getOrElse { throwable ->
            throwable.message ?: "Failed to read the workspace file."
        }
    }
}
