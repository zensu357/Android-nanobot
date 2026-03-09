package com.example.nanobot.domain.repository

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.skills.SkillDefinition

interface SkillRepository {
    suspend fun listSkills(): List<SkillDefinition>
    suspend fun getEnabledSkills(config: AgentConfig): List<SkillDefinition>
}
