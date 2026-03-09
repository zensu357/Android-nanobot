package com.example.nanobot.core.web

import com.example.nanobot.core.model.AgentConfig

interface WebAccessConfigProvider {
    suspend fun getConfig(): AgentConfig
}
