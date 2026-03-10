package com.example.nanobot

import com.example.nanobot.core.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import org.junit.Test

class PayloadTest {
    @Test
    fun testSerialization() {
        val networkJson = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        val req = LlmChatRequest(
            model = "ep-xxxx",
            messages = listOf(
                LlmMessageDto(role = "system", content = JsonPrimitive("System prompt")),
                LlmMessageDto(role = "user", content = JsonPrimitive("Hello"))
            ),
            tools = listOf(
                LlmToolDefinitionDto(
                    type = "function",
                    function = LlmToolFunctionDto(
                        name = "test_tool",
                        description = "Test tool",
                        parameters = JsonObject(emptyMap())
                    )
                )
            ),
            toolChoice = "auto"
        )

        println("=== JSON OUTPUT ===")
        println(networkJson.encodeToString(req))
        println("===================")
    }
}
