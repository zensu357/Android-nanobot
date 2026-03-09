package com.example.nanobot.feature.tools

data class ToolDebugItem(
    val name: String,
    val description: String,
    val schema: String,
    val sampleArguments: String,
    val lastResult: String? = null
)

data class ToolDebugUiState(
    val tools: List<ToolDebugItem> = emptyList(),
    val policySummary: String = "",
    val restrictToWorkspace: Boolean = false,
    val isRunning: Boolean = false,
    val errorMessage: String? = null
)
