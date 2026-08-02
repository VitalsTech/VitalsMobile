package com.vitals.mobile.feature.aiassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.triage.TriageRepository
import com.vitals.mobile.core.data.triage.TriageSessionDto
import com.vitals.mobile.core.session.SessionManager
import com.vitals.mobile.feature.common.UiChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiAssistantUiState(
    val isLoading: Boolean = true,
    val sessionId: String? = null,
    val messages: List<UiChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val triageRepository: TriageRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState = _uiState.asStateFlow()

    init {
        resumeOrCreateSession()
    }

    private fun resumeOrCreateSession() {
        viewModelScope.launch {
            val patientId = sessionManager.currentSession().patientId
            if (patientId == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Профиль пациента не найден")
                return@launch
            }
            runCatching { triageRepository.createSession(patientId) }
                .onSuccess { session -> applySession(session) }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Не удалось открыть чат",
                    )
                }
        }
    }

    private fun applySession(session: TriageSessionDto) {
        val remoteMessages = session.messages.orEmpty().map {
            UiChatMessage(id = it.resolvedId, text = it.resolvedText, fromMe = it.isFromCurrentUser)
        }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            sessionId = session.resolvedId,
            messages = remoteMessages,
            errorMessage = null,
        )
    }

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendCurrentInput() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return
        val sessionId = _uiState.value.sessionId ?: return
        val optimistic = UiChatMessage(id = "local-${System.nanoTime()}", text = text, fromMe = true)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + optimistic,
            inputText = "",
            isSending = true,
        )
        viewModelScope.launch {
            runCatching { triageRepository.sendMessage(sessionId, text) }
            runCatching { triageRepository.getSession(sessionId) }.onSuccess { applySession(it) }
            _uiState.value = _uiState.value.copy(isSending = false)
        }
    }
}
