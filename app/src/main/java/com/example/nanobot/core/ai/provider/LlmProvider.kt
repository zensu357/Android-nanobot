package com.example.nanobot.core.ai.provider

import com.example.nanobot.core.model.LlmChatRequest
import com.example.nanobot.core.model.ProviderChatResult

interface LlmProvider {
    val route: ResolvedProviderRoute

    suspend fun completeChat(request: LlmChatRequest): ProviderChatResult
}
