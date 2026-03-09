package com.example.nanobot.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nanobot.core.ai.PromptPresetCatalog
import com.example.nanobot.core.model.ProviderType
import com.example.nanobot.domain.usecase.CompleteOnboardingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    promptPresetCatalog: PromptPresetCatalog
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        OnboardingUiState(availablePresets = promptPresetCatalog.presets.map { it.id })
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onProviderChanged(value: String) {
        _uiState.value = _uiState.value.copy(providerType = value)
    }

    fun onApiKeyChanged(value: String) {
        _uiState.value = _uiState.value.copy(apiKey = value)
    }

    fun onBaseUrlChanged(value: String) {
        _uiState.value = _uiState.value.copy(baseUrl = value)
    }

    fun onModelChanged(value: String) {
        _uiState.value = _uiState.value.copy(model = value)
    }

    fun onPresetChanged(value: String) {
        _uiState.value = _uiState.value.copy(presetId = value)
    }

    fun onSystemPromptChanged(value: String) {
        _uiState.value = _uiState.value.copy(systemPrompt = value)
    }

    fun complete(onCompleted: () -> Unit) {
        val state = _uiState.value
        if (state.model.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Model is required.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                completeOnboardingUseCase(
                    providerType = state.providerType.ifBlank { ProviderType.OPENAI_COMPATIBLE.wireValue },
                    apiKey = state.apiKey,
                    baseUrl = state.baseUrl,
                    model = state.model,
                    presetId = state.presetId,
                    systemPrompt = state.systemPrompt
                )
            }.onSuccess {
                onCompleted()
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = throwable.message ?: "Failed to complete onboarding."
                )
            }
            _uiState.value = _uiState.value.copy(isSaving = false)
        }
    }
}
