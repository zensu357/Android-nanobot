package com.example.nanobot.core.ai

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.model.AgentProgressEvent
import com.example.nanobot.core.model.AgentRunContext
import com.example.nanobot.core.model.AgentTurnResult
import com.example.nanobot.core.model.Attachment
import com.example.nanobot.core.model.ChatMessage

interface AgentTurnRunner {
    suspend fun runTurn(
        sessionId: String,
        history: List<ChatMessage>,
        userInput: String,
        attachments: List<Attachment>,
        config: AgentConfig,
        runContext: AgentRunContext,
        onProgress: suspend (AgentProgressEvent) -> Unit
    ): AgentTurnResult
}
