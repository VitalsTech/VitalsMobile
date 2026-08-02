package com.vitals.mobile.feature.aiassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.triage.TriageRepository
import com.vitals.mobile.core.data.triage.TriageSessionDto
import com.vitals.mobile.core.session.SessionManager
import com.vitals.mobile.feature.common.UiChatMessage
import com.vitals.mobile.feature.common.optimisticUiChatMessage
import com.vitals.mobile.feature.common.toUiChatMessage
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
    val isCompleting: Boolean = false,
    val readyToComplete: Boolean = false,
    val completeSuggestion: String? = null,
    val completedSessionId: String? = null,
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

    fun retry() = resumeOrCreateSession()

    private fun resumeOrCreateSession() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, completedSessionId = null)
            val session = sessionManager.currentSession()
            val patientId = session.patientId
            if (patientId == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Профиль пациента не найден",
                )
                return@launch
            }

            val existingId = session.triageSessionId
            if (!existingId.isNullOrBlank()) {
                val resumed = runCatching { triageRepository.getSession(existingId) }.getOrNull()
                if (resumed != null) {
                    if (resumed.isCompleted) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            sessionId = resumed.resolvedId,
                            completedSessionId = resumed.resolvedId,
                        )
                        return@launch
                    }
                    applySession(resumed)
                    return@launch
                }
            }

            runCatching { triageRepository.createSession(patientId) }
                .onSuccess { created ->
                    sessionManager.saveTriageSessionId(created.resolvedId)
                    applySession(created)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Не удалось открыть чат триажа",
                    )
                }
        }
    }

    private fun applySession(session: TriageSessionDto) {
        val remoteMessages = session.messages.orEmpty().map { it.toUiChatMessage() }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            sessionId = session.resolvedId,
            messages = remoteMessages,
            readyToComplete = session.resolvedReadyToComplete || session.isCompleted,
            completeSuggestion = session.resolvedCompleteSuggestion,
            errorMessage = null,
            completedSessionId = if (session.isCompleted) session.resolvedId else null,
        )
    }

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendCurrentInput() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return
        val sessionId = _uiState.value.sessionId ?: return
        val optimistic = optimisticUiChatMessage(text)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + optimistic,
            inputText = "",
            isSending = true,
            errorMessage = null,
        )
        viewModelScope.launch {
            runCatching { triageRepository.sendMessage(sessionId, text) }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Не удалось отправить сообщение",
                    )
                }
            runCatching { triageRepository.getSession(sessionId) }
                .onSuccess { applySession(it) }
            _uiState.value = _uiState.value.copy(isSending = false)
        }
    }

    fun completeTriage() {
        val sessionId = _uiState.value.sessionId ?: return
        if (_uiState.value.isCompleting) return
        _uiState.value = _uiState.value.copy(isCompleting = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { triageRepository.completeSession(sessionId) }
                .onSuccess { completed ->
                    sessionManager.saveTriageSessionId(completed.resolvedId)
                    _uiState.value = _uiState.value.copy(
                        isCompleting = false,
                        readyToComplete = true,
                        completedSessionId = completed.resolvedId,
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isCompleting = false,
                        errorMessage = error.message ?: "Не удалось завершить триаж",
                    )
                }
        }
    }

    fun consumeCompletedNavigation() {
        _uiState.value = _uiState.value.copy(completedSessionId = null)
    }
}
