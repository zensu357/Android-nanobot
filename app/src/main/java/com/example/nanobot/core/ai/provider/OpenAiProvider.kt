package com.example.nanobot.core.ai.provider

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.model.LlmChatRequest
import javax.inject.Inject

class OpenAiProvider @Inject constructor(
    private val transport: OpenAiCompatibleProvider
) {
    fun create(config: AgentConfig, route: ResolvedProviderRoute): LlmProvider = object : LlmProvider {
        override val route = route

        override suspend fun completeChat(request: LlmChatRequest): com.example.nanobot.core.model.ProviderChatResult {
            return transport.completeChat(config, route, request)
        }
    }
}
