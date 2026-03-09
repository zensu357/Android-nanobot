package com.example.nanobot.data.mapper

import com.example.nanobot.core.database.entity.ReminderEntity
import com.example.nanobot.core.model.Reminder

fun ReminderEntity.toModel(): Reminder = Reminder(
    id = id,
    title = title,
    message = message,
    triggerAt = triggerAt,
    status = status,
    createdAt = createdAt,
    deliveredAt = deliveredAt,
    errorMessage = errorMessage
)

fun Reminder.toEntity(): ReminderEntity = ReminderEntity(
    id = id,
    title = title,
    message = message,
    triggerAt = triggerAt,
    status = status,
    createdAt = createdAt,
    deliveredAt = deliveredAt,
    errorMessage = errorMessage
)
