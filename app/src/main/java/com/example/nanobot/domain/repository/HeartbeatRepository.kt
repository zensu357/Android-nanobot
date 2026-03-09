package com.example.nanobot.domain.repository

import kotlinx.coroutines.flow.Flow

interface HeartbeatRepository {
    fun observeHeartbeatInstructions(): Flow<String>
    fun observeHeartbeatEnabled(): Flow<Boolean>
    suspend fun getHeartbeatInstructions(): String
    suspend fun isHeartbeatEnabled(): Boolean
    suspend fun setHeartbeatInstructions(value: String)
    suspend fun setHeartbeatEnabled(value: Boolean)
}
