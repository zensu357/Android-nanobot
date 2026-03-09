package com.example.nanobot.core.ai

import com.example.nanobot.core.model.AgentConfig
import javax.inject.Inject

class ModelRouter @Inject constructor() {
    fun resolve(config: AgentConfig): String = config.model.trim().ifBlank { "gpt-4o-mini" }
}
