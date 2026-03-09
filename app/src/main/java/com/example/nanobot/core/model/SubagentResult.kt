package com.example.nanobot.core.model

data class SubagentResult(
    val sessionId: String? = null,
    val parentSessionId: String,
    val subagentDepth: Int,
    val summary: String,
    val artifactPaths: List<String> = emptyList(),
    val completed: Boolean = true
)
