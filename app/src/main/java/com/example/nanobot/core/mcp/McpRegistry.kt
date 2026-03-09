package com.example.nanobot.core.mcp

import com.example.nanobot.core.preferences.McpServerConfigStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

interface McpRegistry {
    fun observeServers(): Flow<List<McpServerDefinition>>
    fun observeCachedTools(): Flow<List<McpToolDescriptor>>
    suspend fun listEnabledServers(): List<McpServerDefinition>
    suspend fun listEnabledTools(): List<McpToolDescriptor>
    suspend fun refreshTools(): McpRefreshResult
    suspend fun saveServers(servers: List<McpServerDefinition>)
    suspend fun callTool(toolName: String, arguments: kotlinx.serialization.json.JsonObject): String
    suspend fun getDiscoverySnapshot(): McpToolDiscoverySnapshot
}

data class McpToolDiscoverySnapshot(
    val enabledServers: List<McpServerDefinition>,
    val tools: List<McpToolDescriptor>
)

@Singleton
class McpRegistryImpl @Inject constructor(
    private val store: McpServerConfigStore,
    private val client: McpClient
) : McpRegistry {
    override fun observeServers(): Flow<List<McpServerDefinition>> = store.observeServers()

    override fun observeCachedTools(): Flow<List<McpToolDescriptor>> = store.observeCachedTools()

    override suspend fun listEnabledServers(): List<McpServerDefinition> =
        store.getServersSnapshot().filter { it.enabled }

    override suspend fun listEnabledTools(): List<McpToolDescriptor> {
        val enabledServers = listEnabledServers().associateBy { it.id }
        return store.getCachedToolsSnapshot().filter { it.serverId in enabledServers }
    }

    override suspend fun getDiscoverySnapshot(): McpToolDiscoverySnapshot {
        return McpToolDiscoverySnapshot(
            enabledServers = listEnabledServers(),
            tools = listEnabledTools()
        )
    }

    override suspend fun refreshTools(): McpRefreshResult {
        val servers = listEnabledServers()
        val previousTools = store.getCachedToolsSnapshot()
        val retainedTools = previousTools.filter { tool -> servers.any { it.id == tool.serverId } }.toMutableList()
        val discoveredByServer = mutableMapOf<String, List<McpToolDescriptor>>()
        val errors = mutableListOf<String>()

        servers.forEach { server ->
            runCatching {
                client.discoverTools(server)
            }.onSuccess { tools ->
                discoveredByServer[server.id] = tools
            }.onFailure { throwable ->
                errors += "${server.label}: ${throwable.message ?: "Discovery failed."}"
            }
        }

        val mergedTools = servers.flatMap { server ->
            discoveredByServer[server.id]
                ?: retainedTools.filter { it.serverId == server.id }
        }
        store.saveCachedTools(mergedTools)
        return McpRefreshResult(
            enabledServerCount = servers.size,
            discoveredToolCount = discoveredByServer.values.sumOf { it.size },
            retainedToolCount = mergedTools.size,
            errors = errors
        )
    }

    override suspend fun saveServers(servers: List<McpServerDefinition>) {
        store.saveServers(servers)
    }

    override suspend fun callTool(toolName: String, arguments: kotlinx.serialization.json.JsonObject): String {
        val descriptor = listEnabledTools().firstOrNull { namespacedToolName(it) == toolName }
            ?: return "MCP tool '$toolName' was not found."
        val server = listEnabledServers().firstOrNull { it.id == descriptor.serverId }
            ?: return "MCP server '${descriptor.serverId}' is not enabled."
        return client.callTool(server, descriptor.remoteName, arguments)
    }

    companion object {
        fun namespacedToolName(tool: McpToolDescriptor): String {
            val safeServerId = tool.serverId.lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_')
            return "mcp.${safeServerId.ifBlank { "server" }}.${tool.name}"
        }
    }
}
