package com.example.nanobot.domain.usecase

import com.example.nanobot.core.ai.AgentTurnRunner
import com.example.nanobot.core.ai.MemoryRefreshScheduler
import com.example.nanobot.core.ai.NoOpMemoryRefreshScheduler
import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.model.AgentProgressEvent
import com.example.nanobot.core.model.AgentRunContext
import com.example.nanobot.core.model.Attachment
import com.example.nanobot.core.model.ChatMessage
import com.example.nanobot.core.model.MessageRole
import com.example.nanobot.domain.repository.SessionRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val agentTurnRunner: AgentTurnRunner,
    private val memoryRefreshScheduler: MemoryRefreshScheduler = NoOpMemoryRefreshScheduler
) {
    suspend operator fun invoke(
        input: String,
        attachments: List<Attachment> = emptyList(),
        config: AgentConfig,
        onProgress: suspend (AgentProgressEvent) -> Unit = {}
    ): List<ChatMessage> {
        val session = sessionRepository.getOrCreateCurrentSession()
        val existingMessages = sessionRepository.getHistoryForModel(session.id)
        val userMessage = ChatMessage(
            sessionId = session.id,
            role = MessageRole.USER,
            content = input,
            attachments = attachments
        )

        sessionRepository.saveMessage(userMessage)

        val turnResult = agentTurnRunner.runTurn(
            sessionId = session.id,
            history = existingMessages,
            userInput = input,
            attachments = attachments,
            config = config,
            runContext = AgentRunContext.root(session.id, config.maxSubagentDepth),
            onProgress = onProgress
        )

        for (message in turnResult.newMessages) {
            sessionRepository.saveMessage(message)
        }
        memoryRefreshScheduler.request(session.id, config)
        sessionRepository.touchSession(
            session.copy(title = input.take(24).ifBlank { "New Chat" }),
            makeCurrent = true
        )

        return buildList {
            add(userMessage)
            addAll(turnResult.newMessages)
        }
    }
}
