package com.example.nanobot.core.tools.impl

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.model.AgentRunContext
import com.example.nanobot.core.tools.AgentTool
import com.example.nanobot.core.tools.ToolAccessCategory
import com.example.nanobot.domain.repository.WorkspaceRepository
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class WriteFileTool @Inject constructor(
    private val workspaceRepository: WorkspaceRepository
) : AgentTool {
    override val name: String = "write_file"
    override val description: String = "Creates or overwrites a UTF-8 text file inside workspace:/ with sandbox enforcement"
    override val accessCategory: ToolAccessCategory = ToolAccessCategory.WORKSPACE_SIDE_EFFECT
    override val availabilityHint: String = "Writes only inside workspace:/; allowed in workspace-restricted mode"
    override val parametersSchema: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("relativePath") {
                put("type", "string")
                put("description", "Workspace-relative file path to create or overwrite")
            }
            putJsonObject("content") {
                put("type", "string")
                put("description", "Full UTF-8 text content to write")
            }
            putJsonObject("overwrite") {
                put("type", "boolean")
                put("description", "Optional flag; must be true to replace an existing file")
            }
        }
        put("required", buildJsonArray {
            add(JsonPrimitive("relativePath"))
            add(JsonPrimitive("content"))
        })
    }

    override suspend fun execute(arguments: JsonObject, config: AgentConfig, runContext: AgentRunContext): String {
        val relativePath = arguments["relativePath"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
        val content = arguments["content"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val overwrite = arguments["overwrite"]?.jsonPrimitive?.booleanOrNull ?: false

        if (relativePath.isBlank()) {
            return "The 'relativePath' field is required for write_file."
        }

        return runCatching {
            val result = workspaceRepository.writeText(relativePath, content, overwrite)
            buildString {
                appendLine("Workspace file written.")
                appendLine("Path: ${result.relativePath}")
                appendLine("Created: ${result.created}")
                appendLine("Bytes Written: ${result.bytesWritten}")
            }.trim()
        }.getOrElse { throwable ->
            throwable.message ?: "Failed to write the workspace file."
        }
    }
}
