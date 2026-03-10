package com.example.nanobot.feature.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nanobot.core.ai.MemoryConsolidator
import com.example.nanobot.core.model.MemoryFact
import com.example.nanobot.core.preferences.SettingsConfigStore
import com.example.nanobot.domain.repository.MemoryRepository
import com.example.nanobot.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val sessionRepository: SessionRepository,
    private val settingsConfigStore: SettingsConfigStore,
    private val memoryConsolidator: MemoryConsolidator
) : ViewModel() {
    private val editorState = MutableStateFlow<MemoryFactEditorState?>(null)
    private val rebuildingSessionIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<MemoryUiState> = combine(
        memoryRepository.observeFacts(),
        memoryRepository.observeSummaries(),
        sessionRepository.observeCurrentSession(),
        editorState,
        rebuildingSessionIds
    ) { facts, summaries, currentSession, editor, rebuilding ->
        MemoryUiState(
            facts = facts,
            summaries = summaries,
            currentSessionId = currentSession?.id,
            editor = editor,
            rebuildingSessionIds = rebuilding
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MemoryUiState())

    fun startEditingFact(fact: MemoryFact) {
        editorState.value = MemoryFactEditorState(
            factId = fact.id,
            draftText = fact.fact
        )
    }

    fun updateFactDraft(value: String) {
        editorState.update { editor ->
            editor?.copy(draftText = value)
        }
    }

    fun cancelEditingFact() {
        editorState.value = null
    }

    fun saveFactEdit() {
        val editor = editorState.value ?: return
        val trimmed = editor.draftText.trim()
        if (trimmed.isBlank()) return

        val fact = uiState.value.facts.firstOrNull { it.id == editor.factId } ?: return
        viewModelScope.launch {
            memoryRepository.upsertFact(
                fact.copy(
                    fact = trimmed.take(220),
                    updatedAt = System.currentTimeMillis()
                )
            )
            editorState.value = null
        }
    }

    fun deleteFact(factId: String) {
        if (editorState.value?.factId == factId) {
            editorState.value = null
        }
        viewModelScope.launch {
            memoryRepository.deleteFact(factId)
        }
    }

    fun deleteSummary(sessionId: String) {
        viewModelScope.launch {
            memoryRepository.deleteSummary(sessionId)
        }
    }

    fun rebuildSummary(sessionId: String) {
        rebuildingSessionIds.update { it + sessionId }
        viewModelScope.launch {
            try {
                val config = settingsConfigStore.configFlow.first()
                val history = sessionRepository.getMessages(sessionId)
                memoryConsolidator.consolidate(sessionId, history, config)
            } finally {
                rebuildingSessionIds.update { it - sessionId }
            }
        }
    }
}
