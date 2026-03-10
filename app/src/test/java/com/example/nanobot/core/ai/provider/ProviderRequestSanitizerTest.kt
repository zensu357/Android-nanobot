package com.example.nanobot.core.ai.provider

import com.example.nanobot.core.model.LlmChatRequest
import com.example.nanobot.core.model.LlmMessageDto
import com.example.nanobot.core.model.LlmToolCallDto
import com.example.nanobot.core.model.LlmToolDefinitionDto
import com.example.nanobot.core.model.LlmToolCallFunctionDto
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProviderRequestSanitizerTest {
    private val sanitizer = ProviderRequestSanitizer()

    @Test
    fun preservesNullAssistantContentWhenToolCallsExist() {
        val request = LlmChatRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                LlmMessageDto(
                    role = "assistant",
                    content = JsonNull,
                    toolCalls = listOf(
                        LlmToolCallDto(
                            id = "tool-call-12345",
                            function = LlmToolCallFunctionDto(
                                name = "notify_user",
                                arguments = "{}"
                            )
                        )
                    )
                )
            ),
            tools = emptyList<LlmToolDefinitionDto>(),
            temperature = 0.2,
            maxTokens = 0
        )

        val sanitized = sanitizer.sanitize(request)

        assertNull(sanitized.messages.first().content)
        assertEquals(1, sanitized.maxTokens)
        assertEquals(9, sanitized.messages.first().toolCalls!!.first().id.length)
    }

    @Test
    fun fillsEmptyUserContent() {
        val request = LlmChatRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                LlmMessageDto(role = "user", content = JsonPrimitive(""))
            )
        )

        val sanitized = sanitizer.sanitize(request)

        assertEquals(" ", sanitized.messages.first().content?.toString()?.trim('"'))
    }
}
