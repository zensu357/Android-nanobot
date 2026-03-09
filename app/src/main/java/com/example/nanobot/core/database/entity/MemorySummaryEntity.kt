package com.example.nanobot.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_summaries")
data class MemorySummaryEntity(
    @PrimaryKey val sessionId: String,
    val summary: String,
    val updatedAt: Long,
    val sourceMessageCount: Int
)
