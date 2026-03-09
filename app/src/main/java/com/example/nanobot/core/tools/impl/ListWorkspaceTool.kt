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

class ListWorkspaceTool @Inject constructor(
    private val workspaceRepository: WorkspaceRepository
) : AgentTool {
    override val name: String = "list_workspace"
    override val description: String = "Lists files and directories inside the sandboxed workspace using relative paths only"
    override val accessCategory: ToolAccessCategory = ToolAccessCategory.WORKSPACE_READ_ONLY
    override val availabilityHint: String = "Read-only access limited to workspace:/"
    override val parametersSchema: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("relativePath") {
                put("type", "string")
                put("description", "Optional workspace-relative directory path; defaults to the workspace root")
            }
            putJsonObject("limit") {
                put("type", "integer")
                put("description", "Optional max number of entries to return; defaults to 50")
            }
        }
    }

    override suspend fun execute(arguments: JsonObject, config: AgentConfig, runContext: AgentRunContext): String {
        val relativePath = arguments["relativePath"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val limit = arguments["limit"]?.jsonPrimitive?.intOrNull ?: 50

        return runCatching {
            val entries = workspaceRepository.list(relativePath, limit)
            if (entries.isEmpty()) {
                return@runCatching "Workspace path ${displayPath(relativePath)} is empty."
            }

            buildString {
                appendLine("Workspace path: ${displayPath(relativePath)}")
                appendLine("Entries:")
                entries.forEach { entry ->
                    val suffix = if (entry.isDirectory) "/" else ""
                    val sizeInfo = entry.sizeBytes?.let { " (${it} bytes)" }.orEmpty()
                    appendLine("- ${entry.relativePath}$suffix$sizeInfo")
                }
            }.trim()
        }.getOrElse { throwable ->
            throwable.message ?: "Failed to list the workspace."
        }
    }

    private fun displayPath(relativePath: String): String {
        val normalized = relativePath.trim().replace('\\', '/').ifBlank { "." }
        return if (normalized == ".") "workspace:/" else "workspace:/$normalized"
    }
}
