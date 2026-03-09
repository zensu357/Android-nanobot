package com.example.nanobot.core.ai

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.model.AgentProgressEvent
import com.example.nanobot.core.model.AgentRunContext
import com.example.nanobot.core.model.AgentTurnResult
import com.example.nanobot.core.model.Attachment
import com.example.nanobot.core.model.ChatMessage
import javax.inject.Inject

class AgentOrchestrator @Inject constructor(
    private val promptComposer: PromptComposer,
    private val toolLoopExecutor: ToolLoopExecutor
) : AgentTurnRunner {
    override
    suspend fun runTurn(
        sessionId: String,
        history: List<ChatMessage>,
        userInput: String,
        attachments: List<Attachment>,
        config: AgentConfig,
        runContext: AgentRunContext,
        onProgress: suspend (AgentProgressEvent) -> Unit
    ): AgentTurnResult {
        val initialMessages = promptComposer.compose(
            runContext = runContext,
            config = config,
            history = history,
            latestUserInput = userInput,
            latestAttachments = attachments
        )
        return toolLoopExecutor.execute(
            sessionId = sessionId,
            initialMessages = initialMessages,
            config = config,
            runContext = runContext,
            onProgress = onProgress
        )
    }
}
