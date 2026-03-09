package com.example.nanobot.core.model

data class MemoryFact(
    val id: String,
    val fact: String,
    val sourceSessionId: String?,
    val createdAt: Long,
    val updatedAt: Long
)

data class MemorySummary(
    val sessionId: String,
    val summary: String,
    val updatedAt: Long,
    val sourceMessageCount: Int = 0
)
