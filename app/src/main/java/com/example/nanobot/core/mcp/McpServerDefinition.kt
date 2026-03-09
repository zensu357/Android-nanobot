package com.example.nanobot.core.mcp

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class McpServerDefinition(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val endpoint: String,
    val enabled: Boolean = true
)
