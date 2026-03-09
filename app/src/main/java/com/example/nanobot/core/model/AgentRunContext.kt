package com.example.nanobot.core.model

data class AgentRunContext(
    val sessionId: String,
    val parentSessionId: String? = null,
    val subagentDepth: Int = 0,
    val maxSubagentDepth: Int = 1
) {
    fun canDelegate(): Boolean = subagentDepth < maxSubagentDepth

    fun child(childSessionId: String): AgentRunContext = AgentRunContext(
        sessionId = childSessionId,
        parentSessionId = sessionId,
        subagentDepth = subagentDepth + 1,
        maxSubagentDepth = maxSubagentDepth
    )

    companion object {
        fun root(sessionId: String, maxSubagentDepth: Int = 1): AgentRunContext = AgentRunContext(
            sessionId = sessionId,
            parentSessionId = null,
            subagentDepth = 0,
            maxSubagentDepth = maxSubagentDepth
        )
    }
}
