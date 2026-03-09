package com.example.nanobot.core.model

data class PromptPreset(
    val id: String,
    val title: String,
    val description: String,
    val systemPrompt: String,
    val behaviorNotes: List<String> = emptyList(),
    val identityOverride: String? = null,
    val operatingRules: List<String> = emptyList(),
    val capabilityHints: List<String> = emptyList()
)
