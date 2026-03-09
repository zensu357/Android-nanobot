package com.example.nanobot.core.skills

data class SkillDefinition(
    val id: String,
    val title: String,
    val description: String,
    val promptFragment: String,
    val recommendedTools: List<String> = emptyList(),
    val tags: List<String> = emptyList()
)
