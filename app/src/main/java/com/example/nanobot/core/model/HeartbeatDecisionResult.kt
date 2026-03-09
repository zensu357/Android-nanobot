package com.example.nanobot.core.model

import kotlinx.serialization.Serializable

@Serializable
data class HeartbeatDecisionResult(
    val action: String,
    val tasks: String = ""
)
