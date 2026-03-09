package com.example.nanobot.feature.onboarding

import com.example.nanobot.core.model.ProviderType

data class OnboardingUiState(
    val providerType: String = ProviderType.OPENAI_COMPATIBLE.wireValue,
    val apiKey: String = "",
    val baseUrl: String = "https://api.openai.com/",
    val model: String = "gpt-4o-mini",
    val presetId: String = "assistant_default",
    val availablePresets: List<String> = emptyList(),
    val systemPrompt: String = "You are Nanobot, a helpful Android-native assistant.",
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)
