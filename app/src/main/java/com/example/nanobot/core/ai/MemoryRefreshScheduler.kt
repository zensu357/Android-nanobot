package com.example.nanobot.core.ai

import com.example.nanobot.core.model.AgentConfig

interface MemoryRefreshScheduler {
    fun request(sessionId: String, config: AgentConfig)
}

object NoOpMemoryRefreshScheduler : MemoryRefreshScheduler {
    override fun request(sessionId: String, config: AgentConfig) = Unit
}
