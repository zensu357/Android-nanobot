package com.example.nanobot.core.tools.impl

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.model.AgentRunContext
import com.example.nanobot.core.tools.AgentTool
import com.example.nanobot.core.tools.ToolAccessCategory
import com.example.nanobot.domain.repository.MemoryRepository
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class MemoryLookupTool @Inject constructor(
    private val memoryRepository: MemoryRepository
) : AgentTool {
    override val name: String = "memory_lookup"
    override val description: String = "Searches saved memory facts and summaries by simple keyword matching"
    override val accessCategory: ToolAccessCategory = ToolAccessCategory.LOCAL_READ_ONLY
    override val availabilityHint: String = "Best used when memory is enabled"
    override val parametersSchema: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("query") {
                put("type", "string")
                put("description", "Keyword query to search within memory")
            }
        }
        put("required", buildJsonArray { add(JsonPrimitive("query")) })
    }

    override suspend fun execute(arguments: JsonObject, config: AgentConfig, runContext: AgentRunContext): String {
        val query = arguments["query"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
        if (query.isBlank()) {
            return "The 'query' field is required for memory_lookup."
        }

        val lowerQuery = query.lowercase()
        val facts = memoryRepository.getFacts()
            .filter { it.fact.lowercase().contains(lowerQuery) }
            .take(5)
        val summaries = memoryRepository.observeSummariesSnapshot()
            .filter { it.summary.lowercase().contains(lowerQuery) }
            .take(3)

        if (facts.isEmpty() && summaries.isEmpty()) {
            return "No memory entries matched '$query'."
        }

        return buildString {
            if (facts.isNotEmpty()) {
                appendLine("Matching facts:")
                facts.forEach { appendLine("- ${it.fact.take(180)}") }
            }
            if (summaries.isNotEmpty()) {
                if (isNotEmpty()) appendLine()
                appendLine("Matching summaries:")
                summaries.forEach { appendLine("- ${it.summary.take(220)}") }
            }
        }.trim()
    }
}
