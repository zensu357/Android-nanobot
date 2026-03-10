package com.example.nanobot.core.network.api

import com.example.nanobot.core.model.LlmChatRequest
import com.example.nanobot.core.model.LlmChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface LlmApi {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Body request: LlmChatRequest
    ): LlmChatResponse
}
