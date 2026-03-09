package com.example.nanobot.core.model

import kotlinx.serialization.Serializable

@Serializable
data class MemoryConsolidationResult(
    val updatedSummary: String,
    val candidateFacts: List<String> = emptyList()
)
