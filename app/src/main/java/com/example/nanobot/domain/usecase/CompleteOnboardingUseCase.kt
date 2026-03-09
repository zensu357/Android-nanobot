package com.example.nanobot.domain.usecase

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.model.ProviderType
import com.example.nanobot.core.preferences.OnboardingStore
import com.example.nanobot.core.preferences.SettingsDataStore
import com.example.nanobot.domain.repository.SessionRepository
import javax.inject.Inject

class CompleteOnboardingUseCase @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val sessionRepository: SessionRepository,
    private val onboardingStore: OnboardingStore
) {
    suspend operator fun invoke(
        providerType: String,
        apiKey: String,
        baseUrl: String,
        model: String,
        presetId: String,
        systemPrompt: String
    ) {
        settingsDataStore.save(
            AgentConfig(
                providerType = ProviderType.from(providerType),
                apiKey = apiKey.trim(),
                baseUrl = baseUrl.trim().ifBlank { "https://api.openai.com/" },
                model = model.trim().ifBlank { "gpt-4o-mini" },
                presetId = presetId.ifBlank { "assistant_default" },
                maxTokens = 4096,
                maxToolIterations = 8,
                memoryWindow = 100,
                reasoningEffort = null,
                enableTools = true,
                enableMemory = true,
                enableBackgroundWork = true,
                webSearchApiKey = "",
                webProxy = "",
                restrictToWorkspace = false,
                systemPrompt = systemPrompt.trim().ifBlank {
                    "You are Nanobot, a helpful Android-native assistant."
                }
            )
        )

        sessionRepository.getOrCreateCurrentSession()
        onboardingStore.setCompleted(true)
    }
}
