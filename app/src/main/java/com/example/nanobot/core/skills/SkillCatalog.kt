package com.example.nanobot.core.skills

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkillCatalog @Inject constructor() {
    val skills: List<SkillDefinition> = listOf(
        SkillDefinition(
            id = "coding_editor",
            title = "Coding Editor",
            description = "Biases Nanobot toward safe code editing, minimal diffs, and verification after changes.",
            promptFragment = "Prefer inspecting the relevant workspace files before editing. When making code changes, keep diffs minimal, preserve existing style, and verify the result with the smallest useful follow-up check.",
            recommendedTools = listOf("list_workspace", "read_file", "search_workspace", "write_file", "replace_in_file"),
            tags = listOf("coding", "workspace")
        ),
        SkillDefinition(
            id = "research_briefing",
            title = "Research Briefing",
            description = "Biases Nanobot toward concise evidence gathering and synthesis across local and web sources.",
            promptFragment = "When researching, gather the smallest sufficient set of local or web facts, cite concrete findings in plain language, and separate confirmed facts from assumptions.",
            recommendedTools = listOf("search_workspace", "web_search", "web_fetch"),
            tags = listOf("research", "web")
        ),
        SkillDefinition(
            id = "planner_mode",
            title = "Planner Mode",
            description = "Biases Nanobot toward explicit execution plans before multi-step work.",
            promptFragment = "For non-trivial tasks, first state a short step-by-step plan, then execute it while keeping the user informed about major state transitions.",
            recommendedTools = emptyList(),
            tags = listOf("planning", "workflow")
        )
    )

    fun getById(id: String): SkillDefinition? = skills.firstOrNull { it.id == id }
}
