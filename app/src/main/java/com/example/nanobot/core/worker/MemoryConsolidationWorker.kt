package com.example.nanobot.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nanobot.core.ai.MemoryConsolidator
import com.example.nanobot.core.preferences.SettingsDataStore
import com.example.nanobot.domain.repository.SessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class MemoryConsolidationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val sessionRepository: SessionRepository,
    private val memoryConsolidator: MemoryConsolidator,
    private val settingsDataStore: SettingsDataStore
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return runCatching {
            val config = settingsDataStore.configFlow.first()
            if (!config.enableMemory) {
                return@runCatching
            }
            val sessions = sessionRepository.observeSessionsSnapshot()
            sessions.forEach { session ->
                val history = sessionRepository.getMessages(session.id)
                if (history.size >= MIN_MESSAGES_FOR_CONSOLIDATION) {
                    val existingSummaryCount = memoryConsolidator.getSummarySourceMessageCount(session.id)
                    if (existingSummaryCount == null || history.size - existingSummaryCount >= MIN_NEW_MESSAGES_DELTA) {
                        memoryConsolidator.consolidate(session.id, history, config)
                    }
                }
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }

    private companion object {
        const val MIN_MESSAGES_FOR_CONSOLIDATION = 6
        const val MIN_NEW_MESSAGES_DELTA = 4
    }
}
