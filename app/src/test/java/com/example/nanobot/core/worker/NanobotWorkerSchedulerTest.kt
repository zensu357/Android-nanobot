package com.example.nanobot.core.worker

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.preferences.SettingsConfigStore
import com.example.nanobot.domain.repository.HeartbeatRepository
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NanobotWorkerSchedulerTest {
    @Test
    fun disablesHeartbeatWhenBackgroundWorkIsOff() = runTest {
        val backend = RecordingScheduleBackend()
        val scheduler = createScheduler(
            config = AgentConfig(enableBackgroundWork = false),
            heartbeatEnabled = true
        )

        scheduler.refreshScheduling(backend)

        assertTrue("heartbeat_work" in backend.cancelled)
        assertFalse(backend.enqueued.contains("heartbeat_work"))
    }

    @Test
    fun enqueuesHeartbeatWhenBackgroundWorkAndHeartbeatAreEnabled() = runTest {
        val backend = RecordingScheduleBackend()
        val scheduler = createScheduler(
            config = AgentConfig(enableBackgroundWork = true),
            heartbeatEnabled = true
        )

        scheduler.refreshScheduling(backend)

        assertTrue("heartbeat_work" in backend.enqueued)
    }

    private fun createScheduler(
        config: AgentConfig,
        heartbeatEnabled: Boolean
    ): NanobotWorkerScheduler {
        return NanobotWorkerScheduler(
            context = androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            settingsDataStore = FakeSettingsConfigStore(config),
            heartbeatRepository = FakeHeartbeatRepository(heartbeatEnabled)
        )
    }

    private class RecordingScheduleBackend : WorkerScheduleBackend {
        val enqueued = mutableListOf<String>()
        val cancelled = mutableListOf<String>()

        override fun enqueueUniquePeriodicWork(
            uniqueName: String,
            policy: androidx.work.ExistingPeriodicWorkPolicy,
            request: androidx.work.PeriodicWorkRequest
        ) {
            enqueued += uniqueName
        }

        override fun cancelUniqueWork(uniqueName: String) {
            cancelled += uniqueName
        }
    }

    private class FakeSettingsConfigStore(config: AgentConfig) : SettingsConfigStore {
        override val configFlow: Flow<AgentConfig> = MutableStateFlow(config)

        override suspend fun save(config: AgentConfig) = Unit
    }

    private class FakeHeartbeatRepository(enabled: Boolean) : HeartbeatRepository {
        private val enabledFlow = MutableStateFlow(enabled)

        override fun observeHeartbeatInstructions(): Flow<String> = MutableStateFlow("")

        override fun observeHeartbeatEnabled(): Flow<Boolean> = enabledFlow

        override suspend fun getHeartbeatInstructions(): String = ""

        override suspend fun isHeartbeatEnabled(): Boolean = enabledFlow.value

        override suspend fun setHeartbeatInstructions(value: String) = Unit

        override suspend fun setHeartbeatEnabled(value: Boolean) {
            enabledFlow.value = value
        }
    }
}
