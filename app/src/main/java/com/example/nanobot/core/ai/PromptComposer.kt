package com.example.nanobot.core.ai

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.model.AgentRunContext
import com.example.nanobot.core.model.Attachment
import com.example.nanobot.core.model.ChatMessage
import com.example.nanobot.core.model.LlmMessageDto
import com.example.nanobot.core.model.MessageRole
import com.example.nanobot.core.ai.provider.ProviderRegistry
import javax.inject.Inject
import kotlinx.serialization.json.JsonPrimitive

class PromptComposer @Inject constructor(
    private val systemPromptBuilder: SystemPromptBuilder,
    private val runtimeContextBuilder: RuntimeContextBuilder,
    private val memoryConsolidator: MemoryConsolidator
) {
    suspend fun compose(
        runContext: AgentRunContext,
        config: AgentConfig,
        history: List<ChatMessage>,
        latestUserInput: String,
        latestAttachments: List<Attachment> = emptyList()
    ): List<LlmMessageDto> {
        val route = ProviderRegistry.resolve(
            providerType = config.providerType,
            apiKey = config.apiKey,
            baseUrl = config.baseUrl,
            model = config.model,
            temperature = config.temperature
        )
        val messages = mutableListOf<LlmMessageDto>()
        val memoryContext = memoryConsolidator.buildMemoryContext(runContext.sessionId)
        messages += LlmMessageDto(
            role = "system",
            content = systemPromptBuilder.build(
                config = config,
                memoryContext = memoryContext
            ).let(::JsonPrimitive)
        )
        history.forEach { message ->
            messages += message.toLlmMessage()
        }
        val runtimeContext = runtimeContextBuilder.build(config, runContext, route)
        messages += LlmMessageDto(
            role = MessageRole.USER.name.lowercase(),
            content = JsonPrimitive(
                runtimeContext + "\n\n" + latestUserInput
            ),
            attachments = latestAttachments.map { attachment ->
                com.example.nanobot.core.model.LlmAttachmentDto(
                    type = attachment.type.name.lowercase(),
                    mimeType = attachment.mimeType,
                    fileName = attachment.displayName,
                    localPath = attachment.localPath
                )
            }
        )
        return messages
    }
}
