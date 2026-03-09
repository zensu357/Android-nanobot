package com.example.nanobot.core.ai

import com.example.nanobot.core.ai.provider.ResolvedProviderRoute
import com.example.nanobot.core.mcp.McpRegistry
import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.model.AgentRunContext
import com.example.nanobot.core.tools.ToolRegistry
import com.example.nanobot.domain.repository.SkillRepository
import com.example.nanobot.domain.repository.WorkspaceRepository
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class RuntimeContextBuilder @Inject constructor(
    private val workspaceRepository: WorkspaceRepository,
    private val toolRegistry: ToolRegistry,
    private val skillRepository: SkillRepository,
    private val mcpRegistry: McpRegistry
) {
    suspend fun build(config: AgentConfig, runContext: AgentRunContext, route: ResolvedProviderRoute): String {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm (EEEE) z"))
        val workspaceRoot = workspaceRepository.getWorkspaceRoot()
        val visibleToolItems = toolRegistry.visibleTools(config, runContext)
        val visibleTools = visibleToolItems.map { it.name }
        val blockedTools = toolRegistry.blockedTools(config, runContext).map { it.name }
        val enabledSkills = skillRepository.getEnabledSkills(config).map { it.id }
        val enabledMcpServers = mcpRegistry.listEnabledServers()
        val enabledMcpTools = mcpRegistry.listEnabledTools()

        return buildString {
            appendLine("[Runtime Metadata - not instructions]")
            appendLine("Current Time: $now")
            appendLine("Session ID: ${runContext.sessionId}")
            appendLine("Parent Session ID: ${runContext.parentSessionId ?: "(none)"}")
            appendLine("Subagent Depth: ${runContext.subagentDepth}")
            appendLine("Max Subagent Depth: ${runContext.maxSubagentDepth}")
            appendLine("Can Delegate Subtasks: ${runContext.canDelegate()}")
            appendLine("Local Orchestration Allowed: ${config.enableTools && visibleToolItems.any { it.accessCategory == com.example.nanobot.core.tools.ToolAccessCategory.LOCAL_ORCHESTRATION }}")
            appendLine("Image Attachments Supported: ${route.supportsImageAttachments}")
            appendLine("Configured Provider: ${config.providerType.wireValue}")
            appendLine("Provider Label: ${route.providerLabel}")
            appendLine("Routed Provider: ${route.spec.name}")
            appendLine("Model: ${route.resolvedModel}")
            appendLine("Temperature: ${route.resolvedTemperature}")
            appendLine("Max Tokens: ${config.maxTokens}")
            appendLine("Max Tool Iterations: ${config.maxToolIterations}")
            appendLine("Enable Tools: ${config.enableTools}")
            appendLine("Tool Access Policy: ${toolRegistry.accessPolicySummary(config)}")
            appendLine("Visible Tools: ${visibleTools.ifEmpty { listOf("(none)") }.joinToString()}")
            if (blockedTools.isNotEmpty()) {
                appendLine("Blocked Tools: ${blockedTools.joinToString()}")
            }
            appendLine("Enabled Skills: ${enabledSkills.ifEmpty { listOf("(none)") }.joinToString()}")
            appendLine("Enabled MCP Servers: ${if (enabledMcpServers.isEmpty()) "(none)" else enabledMcpServers.joinToString { server -> server.label }}")
            appendLine("Dynamic MCP Tools: ${enabledMcpTools.size}")
            appendLine("Web Search Configured: ${config.webSearchApiKey.isNotBlank()}")
            appendLine("Web Proxy Configured: ${config.webProxy.isNotBlank()}")
            appendLine("Enable Memory: ${config.enableMemory}")
            appendLine("Prompt Caching Supported: ${route.supportsPromptCaching}")
            appendLine("Workspace-Restricted Mode: ${config.restrictToWorkspace}")
            appendLine("Workspace Sandbox: ${workspaceRoot.rootId}")
            appendLine("Workspace Available: ${workspaceRoot.isAvailable}")
            appendLine("Workspace Access Mode: ${workspaceRoot.accessMode}")
            appendLine("Workspace Writes Allowed: ${config.enableTools && visibleToolItems.any { it.accessCategory == com.example.nanobot.core.tools.ToolAccessCategory.WORKSPACE_SIDE_EFFECT }}")
        }.trim()
    }
}
