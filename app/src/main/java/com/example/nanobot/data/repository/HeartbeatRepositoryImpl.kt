package com.example.nanobot.data.repository

import com.example.nanobot.core.preferences.HeartbeatStore
import com.example.nanobot.domain.repository.HeartbeatRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Singleton
class HeartbeatRepositoryImpl @Inject constructor(
    private val heartbeatStore: HeartbeatStore
) : HeartbeatRepository {
    override fun observeHeartbeatInstructions(): Flow<String> = heartbeatStore.heartbeatInstructions

    override fun observeHeartbeatEnabled(): Flow<Boolean> = heartbeatStore.heartbeatEnabled

    override suspend fun getHeartbeatInstructions(): String = heartbeatStore.heartbeatInstructions.first()

    override suspend fun isHeartbeatEnabled(): Boolean = heartbeatStore.heartbeatEnabled.first()

    override suspend fun setHeartbeatInstructions(value: String) {
        heartbeatStore.setHeartbeatInstructions(value)
    }

    override suspend fun setHeartbeatEnabled(value: Boolean) {
        heartbeatStore.setHeartbeatEnabled(value)
    }
}
