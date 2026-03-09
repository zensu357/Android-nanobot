package com.example.nanobot.core.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class McpToolDescriptor(
    val serverId: String,
    val serverLabel: String,
    val name: String,
    val remoteName: String,
    val description: String,
    val inputSchema: JsonObject,
    val readOnlyHint: Boolean? = null
)

data class McpRefreshResult(
    val enabledServerCount: Int,
    val discoveredToolCount: Int,
    val retainedToolCount: Int = 0,
    val errors: List<String> = emptyList()
)
