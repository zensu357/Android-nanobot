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

class ReplaceInFileTool @Inject constructor(
    private val workspaceRepository: WorkspaceRepository
) : AgentTool {
    override val name: String = "replace_in_file"
    override val description: String = "Replaces exact text inside a workspace:/ text file with occurrence checks"
    override val accessCategory: ToolAccessCategory = ToolAccessCategory.WORKSPACE_SIDE_EFFECT
    override val availabilityHint: String = "Writes only inside workspace:/; allowed in workspace-restricted mode"
    override val parametersSchema: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("relativePath") {
                put("type", "string")
                put("description", "Workspace-relative file path to modify")
            }
            putJsonObject("find") {
                put("type", "string")
                put("description", "Exact text to find")
            }
            putJsonObject("replaceWith") {
                put("type", "string")
                put("description", "Replacement text")
            }
            putJsonObject("expectedOccurrences") {
                put("type", "integer")
                put("description", "Optional exact number of matches required before replacing")
            }
        }
        put("required", buildJsonArray {
            add(JsonPrimitive("relativePath"))
            add(JsonPrimitive("find"))
            add(JsonPrimitive("replaceWith"))
        })
    }

    override suspend fun execute(arguments: JsonObject, config: AgentConfig, runContext: AgentRunContext): String {
        val relativePath = arguments["relativePath"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
        val find = arguments["find"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val replaceWith = arguments["replaceWith"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val expectedOccurrences = arguments["expectedOccurrences"]?.jsonPrimitive?.intOrNull

        if (relativePath.isBlank()) {
            return "The 'relativePath' field is required for replace_in_file."
        }

        return runCatching {
            val result = workspaceRepository.replaceText(
                relativePath = relativePath,
                find = find,
                replaceWith = replaceWith,
                expectedOccurrences = expectedOccurrences
            )
            buildString {
                appendLine("Workspace file updated.")
                appendLine("Path: ${result.relativePath}")
                appendLine("Replacements: ${result.replacements}")
                appendLine("Bytes Written: ${result.bytesWritten}")
            }.trim()
        }.getOrElse { throwable ->
            throwable.message ?: "Failed to replace text in the workspace file."
        }
    }
}
