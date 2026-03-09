package com.example.nanobot.data.repository

import com.example.nanobot.core.ai.ModelRouter
import com.example.nanobot.core.ai.ProviderFactory
import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.model.LlmChatRequest
import com.example.nanobot.core.model.ProviderChatResult
import com.example.nanobot.domain.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val providerFactory: ProviderFactory,
    private val modelRouter: ModelRouter
) : ChatRepository {
    override suspend fun completeChat(request: LlmChatRequest, config: AgentConfig): ProviderChatResult {
        val normalizedRequest = request.copy(model = modelRouter.resolve(config))
        return providerFactory.create(config).completeChat(normalizedRequest)
    }
}
