package com.example.nanobot.domain.repository

import com.example.nanobot.core.model.MemoryFact
import com.example.nanobot.core.model.MemorySummary
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    fun observeFacts(): Flow<List<MemoryFact>>
    fun observeSummaries(): Flow<List<MemorySummary>>
    suspend fun getFacts(): List<MemoryFact>
    suspend fun getAllSummaries(): List<MemorySummary>
    suspend fun getFactsForQuery(query: String): List<MemoryFact>
    suspend fun observeSummariesSnapshot(): List<MemorySummary>
    suspend fun getSummaryForSession(sessionId: String): MemorySummary?
    suspend fun upsertFact(fact: MemoryFact)
    suspend fun upsertSummary(summary: MemorySummary)
}
