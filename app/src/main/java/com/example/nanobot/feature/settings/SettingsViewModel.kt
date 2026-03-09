package com.example.nanobot.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nanobot.core.ai.PromptPresetCatalog
import com.example.nanobot.core.mcp.McpRegistry
import com.example.nanobot.core.mcp.McpServerDefinition
import com.example.nanobot.core.preferences.SettingsConfigStore
import com.example.nanobot.core.worker.WorkerSchedulingController
import com.example.nanobot.domain.repository.HeartbeatRepository
import com.example.nanobot.domain.repository.SkillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsConfigStore,
    private val promptPresetCatalog: PromptPresetCatalog,
    private val skillRepository: SkillRepository,
    private val mcpRegistry: McpRegistry,
    private val heartbeatRepository: HeartbeatRepository,
    private val nanobotWorkerScheduler: WorkerSchedulingController
) : ViewModel() {
    private val uiStateInternal = MutableStateFlow(
        SettingsUiState(availablePresets = promptPresetCatalog.presets.map { it.id })
    )

    val uiState: StateFlow<SettingsUiState> = uiStateInternal.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsDataStore.configFlow,
                mcpRegistry.observeServers(),
                mcpRegistry.observeCachedTools(),
                heartbeatRepository.observeHeartbeatEnabled(),
                heartbeatRepository.observeHeartbeatInstructions()
            ) { config, mcpServers, mcpTools, heartbeatEnabled, heartbeatInstructions ->
                SettingsBaselineState(
                    config = config,
                    heartbeatEnabled = heartbeatEnabled,
                    heartbeatInstructions = heartbeatInstructions,
                    skills = skillRepository.listSkills(),
                    mcpServers = mcpServers,
                    mcpToolCounts = mcpTools.groupingBy { it.serverId }.eachCount()
                )
            }.collect { baseline ->
                applyBaseline(baseline)
            }
        }
    }

    fun onApiKeyChanged(value: String) = updateDraft { copy(apiKey = value) }

    fun onProviderChanged(value: String) = updateDraft { copy(providerType = value) }

    fun onBaseUrlChanged(value: String) = updateDraft { copy(baseUrl = value) }

    fun onModelChanged(value: String) = updateDraft { copy(model = value) }

    fun onMaxTokensChanged(value: String) = updateDraft { copy(maxTokens = value) }

    fun onMaxToolIterationsChanged(value: String) = updateDraft { copy(maxToolIterations = value) }

    fun onMemoryWindowChanged(value: String) = updateDraft { copy(memoryWindow = value) }

    fun onReasoningEffortChanged(value: String) = updateDraft { copy(reasoningEffort = value) }

    fun onEnableToolsChanged(value: Boolean) = updateDraft { copy(enableTools = value) }

    fun onEnableMemoryChanged(value: Boolean) = updateDraft { copy(enableMemory = value) }

    fun onEnableBackgroundWorkChanged(value: Boolean) = updateDraft { copy(enableBackgroundWork = value) }

    fun onHeartbeatEnabledChanged(value: Boolean) = updateDraft { copy(heartbeatEnabled = value) }

    fun onHeartbeatInstructionsChanged(value: String) = updateDraft { copy(heartbeatInstructions = value) }

    fun onWebSearchApiKeyChanged(value: String) = updateDraft { copy(webSearchApiKey = value) }

    fun onWebProxyChanged(value: String) = updateDraft { copy(webProxy = value) }

    fun onRestrictToWorkspaceChanged(value: Boolean) = updateDraft { copy(restrictToWorkspace = value) }

    fun onPresetChanged(value: String) = updateDraft { copy(presetId = value) }

    fun onSkillToggled(skillId: String, enabled: Boolean) = updateDraft {
        copy(
            skillOptions = skillOptions.map { option ->
                if (option.id == skillId) option.copy(checked = enabled) else option
            }
        )
    }

    fun onDraftMcpLabelChanged(value: String) = updateDraft { copy(draftMcpLabel = value) }

    fun onDraftMcpEndpointChanged(value: String) = updateDraft { copy(draftMcpEndpoint = value) }

    fun onMcpServerToggled(serverId: String, enabled: Boolean) = updateDraft {
        copy(
            mcpServers = mcpServers.map { server ->
                if (server.id == serverId) server.copy(enabled = enabled) else server
            }
        )
    }

    fun addMcpServer() {
        val draft = uiStateInternal.value.draft
        val label = draft.draftMcpLabel.trim()
        val endpoint = draft.draftMcpEndpoint.trim()
        if (label.isBlank() || endpoint.isBlank()) {
            updateUiState { current -> current.copy(mcpStatus = "MCP server label and endpoint are required.") }
            return
        }

        updateDraft(
            status = null
        ) {
            copy(
                mcpServers = mcpServers + McpServerUiState(
                    id = UUID.randomUUID().toString(),
                    label = label,
                    endpoint = endpoint,
                    enabled = true,
                    discoveredToolCount = 0
                ),
                draftMcpLabel = "",
                draftMcpEndpoint = ""
            )
        }
    }

    fun removeMcpServer(serverId: String) = updateDraft(status = null) {
        copy(mcpServers = mcpServers.filterNot { it.id == serverId })
    }

    fun refreshMcpTools() {
        viewModelScope.launch {
            val draft = uiStateInternal.value.draft
            mcpRegistry.saveServers(draft.toMcpServerDefinitions())
            val result = mcpRegistry.refreshTools()
            val status = buildString {
                append("MCP refresh: ${result.discoveredToolCount} newly discovered tool(s), ${result.retainedToolCount} tool(s) available across ${result.enabledServerCount} enabled server(s)")
                if (result.errors.isNotEmpty()) {
                    append(". Errors: ")
                    append(result.errors.joinToString("; "))
                }
            }
            updateUiState { current -> current.copy(mcpStatus = status) }
        }
    }

    fun onSystemPromptChanged(value: String) = updateDraft { copy(systemPrompt = value) }

    fun saveSettings() {
        viewModelScope.launch {
            updateUiState { current -> current.copy(isSaving = true) }
            val draft = uiStateInternal.value.draft
            settingsDataStore.save(draft.toAgentConfig())
            mcpRegistry.saveServers(draft.toMcpServerDefinitions())
            heartbeatRepository.setHeartbeatEnabled(draft.heartbeatEnabled)
            heartbeatRepository.setHeartbeatInstructions(draft.heartbeatInstructions)
            nanobotWorkerScheduler.refreshScheduling()
            updateUiState { current -> current.copy(isSaving = false) }
        }
    }

    fun resetDraft() {
        val baseline = uiStateInternal.value.baseline ?: return
        updateUiState { current ->
            current.copy(
                draft = baseline.toDraftState(),
                isDirty = false,
                mcpStatus = null
            )
        }
    }

    private fun applyBaseline(baseline: SettingsBaselineState) {
        updateUiState { current ->
            if (current.isDirty) {
                current.copy(baseline = baseline)
            } else {
                current.copy(
                    baseline = baseline,
                    draft = baseline.toDraftState(),
                    isDirty = false,
                    mcpStatus = current.mcpStatus?.takeIf { it.isNotBlank() }
                )
            }
        }
    }

    private fun updateDraft(
        status: String? = uiStateInternal.value.mcpStatus,
        transform: SettingsDraftState.() -> SettingsDraftState
    ) {
        updateUiState { current ->
            val updatedDraft = current.draft.transform()
            val baselineSnapshot = current.baseline?.toPersistedSnapshot()
            val draftSnapshot = updatedDraft.toPersistedSnapshot()
            current.copy(
                draft = updatedDraft,
                isDirty = baselineSnapshot == null || draftSnapshot != baselineSnapshot,
                mcpStatus = status
            )
        }
    }

    private fun updateUiState(transform: (SettingsUiState) -> SettingsUiState) {
        uiStateInternal.value = transform(uiStateInternal.value)
    }
}
