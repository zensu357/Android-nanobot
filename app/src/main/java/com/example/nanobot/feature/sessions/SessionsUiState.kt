package com.example.nanobot.feature.sessions

import com.example.nanobot.core.model.ChatSession

data class SessionsUiState(
    val sessions: List<ChatSession> = emptyList(),
    val currentSessionId: String? = null,
    val isCreating: Boolean = false,
    val errorMessage: String? = null
)
