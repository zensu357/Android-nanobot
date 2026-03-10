package com.example.nanobot.feature.memory

import com.example.nanobot.core.model.MemoryFact
import com.example.nanobot.core.model.MemorySummary

data class MemoryFactEditorState(
    val factId: String,
    val draftText: String
)

data class MemoryUiState(
    val facts: List<MemoryFact> = emptyList(),
    val summaries: List<MemorySummary> = emptyList(),
    val currentSessionId: String? = null,
    val editor: MemoryFactEditorState? = null,
    val rebuildingSessionIds: Set<String> = emptySet()
)
