package com.example.nanobot.core.ai

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.domain.repository.SkillRepository
import com.example.nanobot.core.tools.ToolAccessPolicy
import javax.inject.Inject

class SystemPromptBuilder @Inject constructor(
    private val promptPresetCatalog: PromptPresetCatalog,
    private val skillRepository: SkillRepository,
    private val toolAccessPolicy: ToolAccessPolicy
) {
    suspend fun build(config: AgentConfig, memoryContext: String?): String {
        val preset = promptPresetCatalog.getById(config.presetId)
        val enabledSkills = skillRepository.getEnabledSkills(config)
        val sections = listOf(
            PromptSection(
                title = "Identity / Role",
                body = listOf(
                    preset.identityOverride ?: preset.systemPrompt,
                    "Preset: ${preset.title}",
                    preset.description
                )
            ),
            PromptSection(
                title = "Operating Rules",
                body = buildList {
                    add("- Do not pretend a tool has already been executed when it has not.")
                    add("- Before changing, editing, or executing anything, first check the available context.")
                    add("- If you are uncertain, explicitly say so instead of inventing confidence.")
                    add("- If a tool can improve correctness, prefer calling the tool over fabricating its result.")
                    add("- Keep replies direct, concise, and executable.")
                    preset.operatingRules.forEach { add("- $it") }
                }
            ),
            PromptSection(
                title = "Preset Instructions",
                body = buildList {
                    add(preset.systemPrompt.trim())
                    preset.behaviorNotes.forEach { add("- $it") }
                }
            ),
            PromptSection(
                title = "Enabled Skills",
                body = buildList {
                    enabledSkills.forEach { skill ->
                        add("Skill: ${skill.title}")
                        add(skill.promptFragment)
                    }
                }
            ),
            PromptSection(
                title = "Custom User Instructions",
                body = listOf(config.systemPrompt.trim())
            ),
            PromptSection(
                title = "Memory Context",
                body = memoryContext?.lines().orEmpty().filter { it.isNotBlank() }
            ),
            PromptSection(
                title = "Available Capabilities Summary",
                body = buildList {
                    add("- Can operate as an Android-native assistant with persistent sessions.")
                    add("- Can use registered app tools when tool usage is enabled.")
                    add("- ${toolAccessPolicy.describe(config)}")
                    add("- Local orchestration tools may delegate focused subtasks into isolated child runs when available.")
                    add("- Dynamic MCP tools may appear when enabled MCP servers have cached discoveries and current policy allows them.")
                    add("- Image attachments may be available to the model only when the selected provider path supports image attachments.")
                    add("- Can use stored memory summaries and facts when memory is enabled.")
                    add("- Can continue prior conversations within the current session.")
                    preset.capabilityHints.forEach { add("- $it") }
                }
            )
        )

        return SystemPromptContent(sections).render()
    }
}
