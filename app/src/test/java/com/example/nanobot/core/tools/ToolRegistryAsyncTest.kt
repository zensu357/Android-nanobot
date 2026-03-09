package com.example.nanobot.core.tools

import com.example.nanobot.core.mcp.McpRefreshResult
import com.example.nanobot.core.mcp.McpRegistry
import com.example.nanobot.core.mcp.McpServerDefinition
import com.example.nanobot.core.mcp.McpToolDescriptor
import com.example.nanobot.core.mcp.McpToolDiscoverySnapshot
import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.model.AgentRunContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ToolRegistryAsyncTest {
    @Test
    fun visibleToolsIncludesCachedDynamicMcpToolsWithoutBlockingShim() = runTest {
        val registry = ToolRegistry(
            validator = ToolValidator(),
            accessPolicy = ToolAccessPolicy(),
            mcpRegistry = FakeMcpRegistry(
                tools = listOf(
                    McpToolDescriptor(
                        serverId = "github",
                        serverLabel = "GitHub",
                        name = "issue_search",
                        remoteName = "issue-search",
                        description = "Search issues",
                        inputSchema = buildJsonObject { put("type", "object") },
                        readOnlyHint = true
                    )
                )
            )
        )

        val visibleNames = registry.visibleTools(
            config = AgentConfig(enableTools = true),
            runContext = AgentRunContext.root("tool-registry-test")
        ).map { it.name }

        assertEquals(listOf("mcp.github.issue_search"), visibleNames)
    }

    private class FakeMcpRegistry(
        private val tools: List<McpToolDescriptor>
    ) : McpRegistry {
        override fun observeServers(): Flow<List<McpServerDefinition>> = flowOf(emptyList())
        override fun observeCachedTools(): Flow<List<McpToolDescriptor>> = flowOf(tools)
        override suspend fun listEnabledServers(): List<McpServerDefinition> = emptyList()
        override suspend fun listEnabledTools(): List<McpToolDescriptor> = tools
        override suspend fun refreshTools(): McpRefreshResult = McpRefreshResult(0, tools.size, tools.size)
        override suspend fun saveServers(servers: List<McpServerDefinition>) = Unit
        override suspend fun callTool(toolName: String, arguments: JsonObject): String = toolName
        override suspend fun getDiscoverySnapshot(): McpToolDiscoverySnapshot = McpToolDiscoverySnapshot(emptyList(), tools)
    }
}
