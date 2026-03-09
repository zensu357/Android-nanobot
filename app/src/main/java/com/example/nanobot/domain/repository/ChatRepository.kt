package com.example.nanobot.domain.repository

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.model.LlmChatRequest
import com.example.nanobot.core.model.ProviderChatResult

interface ChatRepository {
    suspend fun completeChat(request: LlmChatRequest, config: AgentConfig): ProviderChatResult
}
