package com.example.nanobot.feature.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nanobot.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val sessions = sessionRepository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val currentSession = sessionRepository.observeCurrentSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<SessionsUiState> = combine(
        sessions,
        currentSession
    ) { allSessions, selectedSession ->
        SessionsUiState(
            sessions = allSessions,
            currentSessionId = selectedSession?.id
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionsUiState())

    fun createSession() {
        viewModelScope.launch {
            sessionRepository.createSession()
        }
    }

    fun selectSession(sessionId: String) {
        viewModelScope.launch {
            sessionRepository.selectSession(sessionId)
        }
    }
}
