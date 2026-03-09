package com.example.nanobot.core.mcp

import com.example.nanobot.core.preferences.McpServerConfigStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class McpRegistryTest {
    @Test
    fun refreshCachesDiscoveredToolsAndCallUsesNamespacedToolName() = runTest {
        val store = FakeMcpServerStore(
            servers = listOf(
                McpServerDefinition(
                    id = "github",
                    label = "GitHub",
                    endpoint = "https://mcp.example/github",
                    enabled = true
                )
            )
        )
        val client = FakeMcpClient(
            discoveredTools = mapOf(
                "github" to listOf(
                    McpToolDescriptor(
                        serverId = "github",
                        serverLabel = "GitHub",
                        name = "issue_search",
                        remoteName = "issue-search",
                        description = "Search issues",
                        inputSchema = buildJsonObject {
                            put("type", "object")
                            put("properties", buildJsonObject {})
                        },
                        readOnlyHint = true
                    )
                )
            ),
            callResult = "found 3 issues"
        )
        val registry = McpRegistryImpl(store, client)

        val refresh = registry.refreshTools()
        val tools = registry.listEnabledTools()
        val result = registry.callTool(
            toolName = "mcp.github.issue_search",
            arguments = buildJsonObject { put("query", "nanobot") }
        )

        assertEquals(1, refresh.enabledServerCount)
        assertEquals(1, refresh.discoveredToolCount)
        assertEquals(1, refresh.retainedToolCount)
        assertEquals(1, tools.size)
        assertEquals("found 3 issues", result)
        assertEquals("issue-search", client.lastRemoteToolName)
    }

    @Test
    fun refreshCollectsSchemaFailures() = runTest {
        val store = FakeMcpServerStore(
            servers = listOf(
                McpServerDefinition(
                    id = "bad_server",
                    label = "Bad Server",
                    endpoint = "https://mcp.example/bad",
                    enabled = true
                )
            )
        )
        val client = object : McpClient {
            override suspend fun discoverTools(server: McpServerDefinition): List<McpToolDescriptor> {
                throw IllegalArgumentException("unsupported input schema")
            }

            override suspend fun callTool(server: McpServerDefinition, remoteToolName: String, arguments: JsonObject): String {
                error("unused")
            }
        }
        val registry = McpRegistryImpl(store, client)

        val refresh = registry.refreshTools()

        assertEquals(0, refresh.discoveredToolCount)
        assertEquals(0, refresh.retainedToolCount)
        assertTrue(refresh.errors.single().contains("unsupported input schema"))
    }

    @Test
    fun refreshRetainsPreviousCacheForFailedServer() = runTest {
        val cachedTool = McpToolDescriptor(
            serverId = "github",
            serverLabel = "GitHub",
            name = "issue_search",
            remoteName = "issue-search",
            description = "Search issues",
            inputSchema = buildJsonObject { put("type", "object") },
            readOnlyHint = true
        )
        val store = FakeMcpServerStore(
            servers = listOf(
                McpServerDefinition(
                    id = "github",
                    label = "GitHub",
                    endpoint = "https://mcp.example/github",
                    enabled = true
                )
            ),
            tools = listOf(cachedTool)
        )
        val client = object : McpClient {
            override suspend fun discoverTools(server: McpServerDefinition): List<McpToolDescriptor> {
                throw IllegalStateException("temporary outage")
            }

            override suspend fun callTool(server: McpServerDefinition, remoteToolName: String, arguments: JsonObject): String {
                error("unused")
            }
        }
        val registry = McpRegistryImpl(store, client)

        val refresh = registry.refreshTools()
        val tools = registry.listEnabledTools()

        assertEquals(0, refresh.discoveredToolCount)
        assertEquals(1, refresh.retainedToolCount)
        assertEquals(listOf(cachedTool), tools)
        assertTrue(refresh.errors.single().contains("temporary outage"))
    }

    private class FakeMcpClient(
        private val discoveredTools: Map<String, List<McpToolDescriptor>>,
        private val callResult: String
    ) : McpClient {
        var lastRemoteToolName: String? = null

        override suspend fun discoverTools(server: McpServerDefinition): List<McpToolDescriptor> {
            return discoveredTools[server.id].orEmpty()
        }

        override suspend fun callTool(server: McpServerDefinition, remoteToolName: String, arguments: JsonObject): String {
            lastRemoteToolName = remoteToolName
            return callResult
        }
    }

    private class FakeMcpServerStore(
        servers: List<McpServerDefinition> = emptyList(),
        tools: List<McpToolDescriptor> = emptyList()
    ) : McpServerConfigStore {
        private val serversFlow = MutableStateFlow(servers)
        private val toolsFlow = MutableStateFlow(tools)

        override fun observeServers(): Flow<List<McpServerDefinition>> = serversFlow

        override fun observeCachedTools(): Flow<List<McpToolDescriptor>> = toolsFlow

        override fun getServersSnapshot(): List<McpServerDefinition> = serversFlow.value

        override fun getCachedToolsSnapshot(): List<McpToolDescriptor> = toolsFlow.value

        override suspend fun saveServers(servers: List<McpServerDefinition>) {
            serversFlow.value = servers
        }

        override suspend fun saveCachedTools(tools: List<McpToolDescriptor>) {
            toolsFlow.value = tools
        }
    }
}
