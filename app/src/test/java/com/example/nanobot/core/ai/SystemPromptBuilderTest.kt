package com.example.nanobot.core.ai

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.skills.SkillCatalog
import com.example.nanobot.core.tools.ToolAccessPolicy
import com.example.nanobot.data.repository.SkillRepositoryImpl
import kotlin.test.Test
import kotlin.test.assertTrue

class SystemPromptBuilderTest {
    @Test
    fun includesPresetAndCustomInstructions() {
        val builder = SystemPromptBuilder(
            PromptPresetCatalog(),
            SkillRepositoryImpl(SkillCatalog()),
            ToolAccessPolicy()
        )

        val prompt = kotlinx.coroutines.runBlocking {
            builder.build(
                AgentConfig(
                    presetId = "builder_mode",
                    systemPrompt = "Always explain the implementation plan before coding."
                ),
                memoryContext = null
            )
        }

        assertTrue(prompt.contains("Preset: Builder Mode"))
        assertTrue(prompt.contains("## Preset Instructions"))
        assertTrue(prompt.contains("## Custom User Instructions"))
        assertTrue(prompt.contains("Always explain the implementation plan before coding."))
    }
}
