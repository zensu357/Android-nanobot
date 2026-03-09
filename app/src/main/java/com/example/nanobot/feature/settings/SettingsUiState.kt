package com.example.nanobot.feature.settings

import com.example.nanobot.core.mcp.McpServerDefinition
import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.model.ProviderType
import com.example.nanobot.core.skills.SkillDefinition

data class SkillOptionUiState(
    val id: String,
    val title: String,
    val description: String,
    val checked: Boolean,
    val tags: List<String> = emptyList()
)

data class McpServerUiState(
    val id: String,
    val label: String,
    val endpoint: String,
    val enabled: Boolean,
    val discoveredToolCount: Int = 0
)

data class SettingsDraftState(
    val providerType: String = ProviderType.OPENAI_COMPATIBLE.wireValue,
    val apiKey: String = "",
    val baseUrl: String = "",
    val model: String = "",
    val maxTokens: String = "4096",
    val maxToolIterations: String = "8",
    val memoryWindow: String = "100",
    val reasoningEffort: String = "",
    val enableTools: Boolean = true,
    val enableMemory: Boolean = true,
    val enableBackgroundWork: Boolean = true,
    val heartbeatEnabled: Boolean = true,
    val heartbeatInstructions: String = "",
    val webSearchApiKey: String = "",
    val webProxy: String = "",
    val restrictToWorkspace: Boolean = false,
    val presetId: String = "assistant_default",
    val skillOptions: List<SkillOptionUiState> = emptyList(),
    val mcpServers: List<McpServerUiState> = emptyList(),
    val draftMcpLabel: String = "",
    val draftMcpEndpoint: String = "",
    val systemPrompt: String = ""
)

data class SettingsBaselineState(
    val config: AgentConfig,
    val heartbeatEnabled: Boolean,
    val heartbeatInstructions: String,
    val skills: List<SkillDefinition>,
    val mcpServers: List<McpServerDefinition>,
    val mcpToolCounts: Map<String, Int>
)

data class PersistedSettingsSnapshot(
    val agentConfig: AgentConfig,
    val heartbeatEnabled: Boolean,
    val heartbeatInstructions: String,
    val mcpServers: List<McpServerDefinition>
)

data class SettingsUiState(
    val draft: SettingsDraftState = SettingsDraftState(),
    val baseline: SettingsBaselineState? = null,
    val availablePresets: List<String> = emptyList(),
    val mcpStatus: String? = null,
    val isDirty: Boolean = false,
    val isSaving: Boolean = false
) {
    val providerType: String get() = draft.providerType
    val apiKey: String get() = draft.apiKey
    val baseUrl: String get() = draft.baseUrl
    val model: String get() = draft.model
    val maxTokens: String get() = draft.maxTokens
    val maxToolIterations: String get() = draft.maxToolIterations
    val memoryWindow: String get() = draft.memoryWindow
    val reasoningEffort: String get() = draft.reasoningEffort
    val enableTools: Boolean get() = draft.enableTools
    val enableMemory: Boolean get() = draft.enableMemory
    val enableBackgroundWork: Boolean get() = draft.enableBackgroundWork
    val heartbeatEnabled: Boolean get() = draft.heartbeatEnabled
    val heartbeatInstructions: String get() = draft.heartbeatInstructions
    val webSearchApiKey: String get() = draft.webSearchApiKey
    val webProxy: String get() = draft.webProxy
    val restrictToWorkspace: Boolean get() = draft.restrictToWorkspace
    val presetId: String get() = draft.presetId
    val skillOptions: List<SkillOptionUiState> get() = draft.skillOptions
    val mcpServers: List<McpServerUiState> get() = draft.mcpServers
    val draftMcpLabel: String get() = draft.draftMcpLabel
    val draftMcpEndpoint: String get() = draft.draftMcpEndpoint
    val systemPrompt: String get() = draft.systemPrompt
}

fun SettingsBaselineState.toDraftState(): SettingsDraftState {
    return SettingsDraftState(
        providerType = config.providerType.wireValue,
        apiKey = config.apiKey,
        baseUrl = config.baseUrl,
        model = config.model,
        maxTokens = config.maxTokens.toString(),
        maxToolIterations = config.maxToolIterations.toString(),
        memoryWindow = config.memoryWindow.toString(),
        reasoningEffort = config.reasoningEffort.orEmpty(),
        enableTools = config.enableTools,
        enableMemory = config.enableMemory,
        enableBackgroundWork = config.enableBackgroundWork,
        heartbeatEnabled = heartbeatEnabled,
        heartbeatInstructions = heartbeatInstructions,
        webSearchApiKey = config.webSearchApiKey,
        webProxy = config.webProxy,
        restrictToWorkspace = config.restrictToWorkspace,
        presetId = config.presetId,
        skillOptions = skills.map { skill ->
            SkillOptionUiState(
                id = skill.id,
                title = skill.title,
                description = skill.description,
                checked = skill.id in config.enabledSkillIds,
                tags = skill.tags
            )
        },
        mcpServers = mcpServers.map { server ->
            McpServerUiState(
                id = server.id,
                label = server.label,
                endpoint = server.endpoint,
                enabled = server.enabled,
                discoveredToolCount = mcpToolCounts[server.id] ?: 0
            )
        },
        draftMcpLabel = "",
        draftMcpEndpoint = "",
        systemPrompt = config.systemPrompt
    )
}

fun SettingsDraftState.toAgentConfig(): AgentConfig {
    return AgentConfig(
        providerType = ProviderType.from(providerType),
        apiKey = apiKey,
        baseUrl = baseUrl,
        model = model,
        maxTokens = maxTokens.toIntOrNull() ?: 4096,
        maxToolIterations = maxToolIterations.toIntOrNull() ?: 8,
        memoryWindow = memoryWindow.toIntOrNull() ?: 100,
        reasoningEffort = reasoningEffort.ifBlank { null },
        enableTools = enableTools,
        enableMemory = enableMemory,
        enableBackgroundWork = enableBackgroundWork,
        webSearchApiKey = webSearchApiKey,
        webProxy = webProxy,
        restrictToWorkspace = restrictToWorkspace,
        presetId = presetId,
        enabledSkillIds = skillOptions.filter { it.checked }.map { it.id },
        systemPrompt = systemPrompt
    )
}

fun SettingsDraftState.toMcpServerDefinitions(): List<McpServerDefinition> {
    return mcpServers.map { server ->
        McpServerDefinition(
            id = server.id,
            label = server.label,
            endpoint = server.endpoint,
            enabled = server.enabled
        )
    }
}

fun SettingsBaselineState.toPersistedSnapshot(): PersistedSettingsSnapshot {
    return PersistedSettingsSnapshot(
        agentConfig = config,
        heartbeatEnabled = heartbeatEnabled,
        heartbeatInstructions = heartbeatInstructions,
        mcpServers = mcpServers
    )
}

fun SettingsDraftState.toPersistedSnapshot(): PersistedSettingsSnapshot {
    return PersistedSettingsSnapshot(
        agentConfig = toAgentConfig(),
        heartbeatEnabled = heartbeatEnabled,
        heartbeatInstructions = heartbeatInstructions,
        mcpServers = toMcpServerDefinitions()
    )
}
