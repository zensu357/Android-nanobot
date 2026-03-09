package com.example.nanobot.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val parentSessionId: String?,
    val subagentDepth: Int,
    val createdAt: Long,
    val updatedAt: Long
)
