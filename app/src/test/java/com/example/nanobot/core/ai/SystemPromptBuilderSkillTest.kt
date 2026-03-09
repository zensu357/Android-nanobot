package com.example.nanobot.core.ai

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.skills.SkillCatalog
import com.example.nanobot.core.tools.ToolAccessPolicy
import com.example.nanobot.data.repository.SkillRepositoryImpl
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SystemPromptBuilderSkillTest {
    @Test
    fun enabledSkillIsInjectedIntoPrompt() {
        val builder = SystemPromptBuilder(
            promptPresetCatalog = PromptPresetCatalog(),
            skillRepository = SkillRepositoryImpl(SkillCatalog()),
            toolAccessPolicy = ToolAccessPolicy()
        )

        val prompt = kotlinx.coroutines.runBlocking {
            builder.build(
                AgentConfig(enabledSkillIds = listOf("coding_editor")),
                memoryContext = null
            )
        }

        assertTrue(prompt.contains("## Enabled Skills"))
        assertTrue(prompt.contains("Skill: Coding Editor"))
        assertTrue(prompt.contains("Prefer inspecting the relevant workspace files before editing."))
    }

    @Test
    fun disabledSkillDoesNotAppearInPrompt() {
        val builder = SystemPromptBuilder(
            promptPresetCatalog = PromptPresetCatalog(),
            skillRepository = SkillRepositoryImpl(SkillCatalog()),
            toolAccessPolicy = ToolAccessPolicy()
        )

        val prompt = kotlinx.coroutines.runBlocking {
            builder.build(
                AgentConfig(enabledSkillIds = emptyList()),
                memoryContext = null
            )
        }

        assertFalse(prompt.contains("Skill: Coding Editor"))
    }
}
