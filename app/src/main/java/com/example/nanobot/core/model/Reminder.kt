package com.example.nanobot.core.model

object ReminderStatus {
    const val SCHEDULED = "scheduled"
    const val DELIVERED = "delivered"
    const val FAILED = "failed"
    const val CANCELLED = "cancelled"
}

data class Reminder(
    val id: String,
    val title: String?,
    val message: String,
    val triggerAt: Long,
    val status: String,
    val createdAt: Long,
    val deliveredAt: Long? = null,
    val errorMessage: String? = null
)
