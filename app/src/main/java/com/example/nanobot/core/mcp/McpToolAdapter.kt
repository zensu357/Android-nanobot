package com.example.nanobot.core.mcp

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.model.AgentRunContext
import com.example.nanobot.core.tools.AgentTool
import com.example.nanobot.core.tools.ToolAccessCategory
import kotlinx.serialization.json.JsonObject

class McpToolAdapter(
    private val descriptor: McpToolDescriptor,
    private val registry: McpRegistry
) : AgentTool {
    override val name: String = McpRegistryImpl.namespacedToolName(descriptor)
    override val description: String = descriptor.description
    override val accessCategory: ToolAccessCategory = if (descriptor.readOnlyHint == true) {
        ToolAccessCategory.EXTERNAL_READ_ONLY
    } else {
        ToolAccessCategory.EXTERNAL_SIDE_EFFECT
    }
    override val availabilityHint: String = "Remote MCP tool from ${descriptor.serverLabel}"
    override val parametersSchema: JsonObject = descriptor.inputSchema

    override suspend fun execute(arguments: JsonObject, config: AgentConfig, runContext: AgentRunContext): String {
        return registry.callTool(name, arguments)
    }
}
