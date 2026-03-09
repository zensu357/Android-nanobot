package com.example.nanobot.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val title: String?,
    val message: String,
    val triggerAt: Long,
    val status: String,
    val createdAt: Long,
    val deliveredAt: Long?,
    val errorMessage: String?
)
