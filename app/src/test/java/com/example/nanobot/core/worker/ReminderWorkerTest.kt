package com.example.nanobot.core.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.nanobot.core.model.Reminder
import com.example.nanobot.core.model.ReminderStatus
import com.example.nanobot.core.notifications.ReminderNotificationSink
import com.example.nanobot.domain.repository.ReminderRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReminderWorkerTest {
    @Test
    fun deliversOnlyDueRemindersAndMarksFailures() = runTest {
        val repository = FakeReminderRepository(
            dueReminders = listOf(
                Reminder("due-1", "Title", "Now", 1L, ReminderStatus.SCHEDULED, 0L),
                Reminder("due-2", null, "Fails", 1L, ReminderStatus.SCHEDULED, 0L)
            )
        )
        val notifier = RecordingReminderNotifier(failIds = setOf("due-2"))
        val workerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker {
                return ReminderWorker(
                    appContext = appContext,
                    workerParams = workerParameters,
                    reminderRepository = repository,
                    reminderNotifier = notifier
                )
            }
        }
        val worker = androidx.work.testing.TestListenableWorkerBuilder<ReminderWorker>(appContext())
            .setWorkerFactory(workerFactory)
            .build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(repository.deliveredIds.contains("due-1"))
        assertTrue(repository.failedIds.contains("due-2"))
        assertEquals(listOf("due-1", "due-2"), notifier.notifiedIds)
    }

    private fun appContext(): Context = ApplicationProvider.getApplicationContext()

    private class FakeReminderRepository(
        private val dueReminders: List<Reminder>
    ) : ReminderRepository {
        val deliveredIds = mutableListOf<String>()
        val failedIds = mutableListOf<String>()

        override fun observeReminders(): Flow<List<Reminder>> = flowOf(dueReminders)
        override suspend fun getReminders(): List<Reminder> = dueReminders
        override suspend fun getDueReminders(now: Long): List<Reminder> = dueReminders
        override suspend fun markDelivered(id: String, deliveredAt: Long) { deliveredIds += id }
        override suspend fun markFailed(id: String, errorMessage: String) { failedIds += id }
        override suspend fun upsert(reminder: Reminder) = Unit
    }

    private class RecordingReminderNotifier(
        private val failIds: Set<String>
    ) : ReminderNotificationSink {
        val notifiedIds = mutableListOf<String>()

        override fun notify(reminder: Reminder) {
            notifiedIds += reminder.id
            if (reminder.id in failIds) {
                throw IllegalStateException("notification failed")
            }
        }
    }
}
