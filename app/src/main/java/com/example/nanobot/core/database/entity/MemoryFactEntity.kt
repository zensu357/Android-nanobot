package com.example.nanobot.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_facts")
data class MemoryFactEntity(
    @PrimaryKey val id: String,
    val fact: String,
    val sourceSessionId: String?,
    val createdAt: Long,
    val updatedAt: Long
)
