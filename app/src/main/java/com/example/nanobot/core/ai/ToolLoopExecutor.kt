package com.example.nanobot.core.ai

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.model.AgentProgressEvent
import com.example.nanobot.core.model.AgentRunContext
import com.example.nanobot.core.model.AgentTurnResult
import com.example.nanobot.core.model.ChatMessage
import com.example.nanobot.core.model.LlmChatRequest
import com.example.nanobot.core.model.MessageRole
import com.example.nanobot.domain.repository.ChatRepository
import com.example.nanobot.core.tools.ToolRegistry
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class ToolLoopExecutor @Inject constructor(
    private val chatRepository: ChatRepository,
    private val toolRegistry: ToolRegistry
) {
    suspend fun execute(
        sessionId: String,
        initialMessages: List<com.example.nanobot.core.model.LlmMessageDto>,
        config: AgentConfig,
        runContext: AgentRunContext = AgentRunContext.root(sessionId, config.maxSubagentDepth),
        maxIterations: Int = config.maxToolIterations,
        onProgress: suspend (AgentProgressEvent) -> Unit = {}
    ): AgentTurnResult {
        val messages = initialMessages.toMutableList()
        val emittedMessages = mutableListOf<ChatMessage>()
        val visibleToolNames = toolRegistry.visibleTools(config, runContext).map { it.name }.toSet()
        val availableTools = toolRegistry.getDefinitions(config, runContext).ifEmpty { null }

        onProgress(AgentProgressEvent.Started)

        repeat(maxIterations) {
            onProgress(AgentProgressEvent.Thinking)
            val response = chatRepository.completeChat(
                request = LlmChatRequest(
                    model = config.model,
                    messages = messages,
                    temperature = config.temperature,
                    maxTokens = config.maxTokens,
                    tools = if (config.enableTools) availableTools else null,
                    toolChoice = if (config.enableTools && availableTools != null) "auto" else null
                ),
                config = config
            )

            val assistantMessage = response.toAssistantMessage(sessionId)
            emittedMessages += assistantMessage
            messages += assistantMessage.toLlmMessage()

            if (response.toolCalls.isEmpty()) {
                onProgress(AgentProgressEvent.Finishing)
                onProgress(AgentProgressEvent.Completed)
                return AgentTurnResult(
                    newMessages = emittedMessages,
                    finalResponse = assistantMessage
                )
            }

            response.toolCalls.forEach { toolCall ->
                onProgress(AgentProgressEvent.ToolCalling(toolCall.name))
                if (toolCall.name !in visibleToolNames) {
                    onProgress(AgentProgressEvent.Error("Tool '${toolCall.name}' is blocked by the current tool access policy."))
                }
                val result = try {
                    toolRegistry.execute(toolCall.name, toolCall.arguments, config, runContext)
                } catch (throwable: Throwable) {
                    if (throwable is CancellationException) throw throwable
                    onProgress(AgentProgressEvent.Error(throwable.message ?: "Tool execution failed."))
                    throw throwable
                }
                val toolMessage = ChatMessage(
                    sessionId = sessionId,
                    role = MessageRole.TOOL,
                    content = result,
                    toolCallId = toolCall.id,
                    toolName = toolCall.name
                )
                emittedMessages += toolMessage
                messages += toolMessage.toLlmMessage()
                onProgress(AgentProgressEvent.ToolResult(toolCall.name))
            }
        }

        onProgress(AgentProgressEvent.Error("Maximum tool-iteration limit reached."))
        val fallback = ChatMessage(
            sessionId = sessionId,
            role = MessageRole.ASSISTANT,
            content = "I reached the maximum tool-iteration limit before finishing this task."
        )
        return AgentTurnResult(
            newMessages = emittedMessages + fallback,
            finalResponse = fallback
        )
    }
}
