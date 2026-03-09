package com.example.nanobot.feature.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nanobot.domain.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MemoryViewModel @Inject constructor(
    memoryRepository: MemoryRepository
) : ViewModel() {
    val uiState: StateFlow<MemoryUiState> = combine(
        memoryRepository.observeFacts(),
        memoryRepository.observeSummaries()
    ) { facts, summaries ->
        MemoryUiState(
            facts = facts,
            summaries = summaries
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MemoryUiState())
}
