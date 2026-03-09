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

class SearchWorkspaceTool @Inject constructor(
    private val workspaceRepository: WorkspaceRepository
) : AgentTool {
    override val name: String = "search_workspace"
    override val description: String = "Searches text files inside the sandboxed workspace and returns compact line matches"
    override val accessCategory: ToolAccessCategory = ToolAccessCategory.WORKSPACE_READ_ONLY
    override val availabilityHint: String = "Read-only access limited to workspace:/"
    override val parametersSchema: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("query") {
                put("type", "string")
                put("description", "Case-insensitive text query to search for")
            }
            putJsonObject("relativePath") {
                put("type", "string")
                put("description", "Optional workspace-relative directory or file path scope")
            }
            putJsonObject("limit") {
                put("type", "integer")
                put("description", "Optional max number of search hits to return; defaults to 10")
            }
        }
        put("required", buildJsonArray { add(JsonPrimitive("query")) })
    }

    override suspend fun execute(arguments: JsonObject, config: AgentConfig, runContext: AgentRunContext): String {
        val query = arguments["query"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
        val relativePath = arguments["relativePath"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val limit = arguments["limit"]?.jsonPrimitive?.intOrNull ?: 10

        if (query.isBlank()) {
            return "The 'query' field is required for search_workspace."
        }

        return runCatching {
            val hits = workspaceRepository.search(query, relativePath, limit)
            if (hits.isEmpty()) {
                return@runCatching "No workspace matches found for '$query' in ${displayPath(relativePath)}."
            }

            buildString {
                appendLine("Workspace query: $query")
                appendLine("Scope: ${displayPath(relativePath)}")
                appendLine("Matches:")
                hits.forEach { hit ->
                    appendLine("- ${hit.relativePath}:${hit.lineNumber}: ${hit.snippet}")
                }
            }.trim()
        }.getOrElse { throwable ->
            throwable.message ?: "Failed to search the workspace."
        }
    }

    private fun displayPath(relativePath: String): String {
        val normalized = relativePath.trim().replace('\\', '/').ifBlank { "." }
        return if (normalized == ".") "workspace:/" else "workspace:/$normalized"
    }
}
