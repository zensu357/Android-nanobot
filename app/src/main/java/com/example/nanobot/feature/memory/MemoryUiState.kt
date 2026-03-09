package com.example.nanobot.feature.memory

import com.example.nanobot.core.model.MemoryFact
import com.example.nanobot.core.model.MemorySummary

data class MemoryUiState(
    val facts: List<MemoryFact> = emptyList(),
    val summaries: List<MemorySummary> = emptyList()
)
