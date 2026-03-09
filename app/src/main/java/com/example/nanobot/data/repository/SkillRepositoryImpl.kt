package com.example.nanobot.data.repository

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.skills.SkillCatalog
import com.example.nanobot.core.skills.SkillDefinition
import com.example.nanobot.domain.repository.SkillRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkillRepositoryImpl @Inject constructor(
    private val skillCatalog: SkillCatalog
) : SkillRepository {
    override suspend fun listSkills(): List<SkillDefinition> = skillCatalog.skills

    override suspend fun getEnabledSkills(config: AgentConfig): List<SkillDefinition> {
        val enabledIds = config.enabledSkillIds.toSet()
        return skillCatalog.skills.filter { it.id in enabledIds }
    }
}
