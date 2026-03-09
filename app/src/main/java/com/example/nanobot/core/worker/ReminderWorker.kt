package com.example.nanobot.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nanobot.core.notifications.ReminderNotificationSink
import com.example.nanobot.domain.repository.ReminderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminderRepository: ReminderRepository,
    private val reminderNotifier: ReminderNotificationSink
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val dueReminders = reminderRepository.getDueReminders(now)

        dueReminders.forEach { reminder ->
            runCatching {
                reminderNotifier.notify(reminder)
                reminderRepository.markDelivered(reminder.id, now)
            }.onFailure { throwable ->
                reminderRepository.markFailed(
                    reminder.id,
                    throwable.message ?: "Unknown reminder delivery failure."
                )
            }
        }

        return Result.success()
    }
}
