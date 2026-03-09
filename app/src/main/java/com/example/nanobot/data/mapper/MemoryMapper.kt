package com.example.nanobot.data.mapper

import com.example.nanobot.core.database.entity.MemoryFactEntity
import com.example.nanobot.core.database.entity.MemorySummaryEntity
import com.example.nanobot.core.model.MemoryFact
import com.example.nanobot.core.model.MemorySummary

fun MemoryFactEntity.toModel(): MemoryFact = MemoryFact(
    id = id,
    fact = fact,
    sourceSessionId = sourceSessionId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MemoryFact.toEntity(): MemoryFactEntity = MemoryFactEntity(
    id = id,
    fact = fact,
    sourceSessionId = sourceSessionId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MemorySummaryEntity.toModel(): MemorySummary = MemorySummary(
    sessionId = sessionId,
    summary = summary,
    updatedAt = updatedAt,
    sourceMessageCount = sourceMessageCount
)

fun MemorySummary.toEntity(): MemorySummaryEntity = MemorySummaryEntity(
    sessionId = sessionId,
    summary = summary,
    updatedAt = updatedAt,
    sourceMessageCount = sourceMessageCount
)
