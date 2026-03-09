package com.example.nanobot.core.model

sealed interface AgentProgressEvent {
    data object Started : AgentProgressEvent
    data object Thinking : AgentProgressEvent
    data class ToolCalling(val toolName: String) : AgentProgressEvent
    data class ToolResult(val toolName: String) : AgentProgressEvent
    data object Finishing : AgentProgressEvent
    data object Completed : AgentProgressEvent
    data object Cancelled : AgentProgressEvent
    data class Error(val message: String) : AgentProgressEvent
}
