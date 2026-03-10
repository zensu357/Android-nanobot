package com.example.nanobot.core.ai

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.domain.repository.SessionRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Singleton
class RealtimeMemoryRefreshScheduler @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val memoryConsolidator: MemoryConsolidator
) : MemoryRefreshScheduler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingJobs = ConcurrentHashMap<String, Job>()
    private val latestConfigs = ConcurrentHashMap<String, AgentConfig>()
    private val dirtySessions = ConcurrentHashMap.newKeySet<String>()
    private val runningSessions = ConcurrentHashMap.newKeySet<String>()

    override fun request(sessionId: String, config: AgentConfig) {
        if (!config.enableMemory) return

        latestConfigs[sessionId] = config
        dirtySessions += sessionId

        if (runningSessions.contains(sessionId)) {
            return
        }

        pendingJobs.remove(sessionId)?.cancel()
        pendingJobs[sessionId] = scope.launch {
            delay(REALTIME_DEBOUNCE_MS)
            pendingJobs.remove(sessionId)
            runRefreshLoop(sessionId)
        }
    }

    private suspend fun runRefreshLoop(sessionId: String) {
        if (!runningSessions.add(sessionId)) {
            return
        }

        try {
            while (true) {
                dirtySessions.remove(sessionId)
                val config = latestConfigs[sessionId] ?: break
                val history = sessionRepository.getMessages(sessionId)
                val shouldConsolidate = memoryConsolidator.shouldConsolidate(
                    sessionId = sessionId,
                    historySize = history.size,
                    config = config,
                    minMessages = REALTIME_MIN_MESSAGES,
                    minNewMessagesDelta = REALTIME_MIN_NEW_MESSAGES_DELTA
                )
                if (!shouldConsolidate) {
                    break
                }
                memoryConsolidator.consolidate(sessionId, history, config)
                if (!dirtySessions.contains(sessionId)) {
                    break
                }
            }
        } finally {
            runningSessions.remove(sessionId)
            if (dirtySessions.contains(sessionId)) {
                latestConfigs[sessionId]?.let { latestConfig ->
                    request(sessionId, latestConfig)
                }
            }
        }
    }

    private companion object {
        const val REALTIME_DEBOUNCE_MS = 8_000L
        const val REALTIME_MIN_MESSAGES = 4
        const val REALTIME_MIN_NEW_MESSAGES_DELTA = 2
    }
}
