package com.vitals.mobile.feature.triage

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

data class TriageUiState(
    val isLoading: Boolean = true,
    val sessionId: String? = null,
    val messages: List<UiChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val completedSessionId: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class TriageViewModel @Inject constructor(
    private val triageRepository: TriageRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TriageUiState())
    val uiState = _uiState.asStateFlow()

    init {
        startSession()
    }

    private fun startSession() {
        viewModelScope.launch {
            val patientId = sessionManager.currentSession().patientId
            if (patientId == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Профиль пациента не найден")
                return@launch
            }
            runCatching { triageRepository.createSession(patientId) }
                .onSuccess { session ->
                    sessionManager.saveTriageSessionId(session.resolvedId)
                    applySession(session)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Не удалось начать триаж",
                    )
                }
        }
    }

    fun startNewTriage() {
        viewModelScope.launch {
            _uiState.value = TriageUiState(isLoading = true)
            sessionManager.saveTriageSessionId(null)
            val patientId = sessionManager.currentSession().patientId
            if (patientId == null) {
                _uiState.value = TriageUiState(
                    isLoading = false,
                    errorMessage = "Профиль пациента не найден",
                )
                return@launch
            }
            runCatching { triageRepository.createSession(patientId) }
                .onSuccess { session ->
                    sessionManager.saveTriageSessionId(session.resolvedId)
                    applySession(session)
                }
                .onFailure { error ->
                    _uiState.value = TriageUiState(
                        isLoading = false,
                        errorMessage = error.message ?: "Не удалось начать новый триаж",
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
            errorMessage = null,
        )
    }

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendQuickReply(text: String) = send(text)

    fun sendCurrentInput() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return
        send(text)
        _uiState.value = _uiState.value.copy(inputText = "")
    }

    private fun send(text: String) {
        val sessionId = _uiState.value.sessionId ?: return
        val optimistic = optimisticUiChatMessage(text)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + optimistic,
            isSending = true,
        )
        viewModelScope.launch {
            runCatching { triageRepository.sendMessage(sessionId, text) }
            runCatching { triageRepository.getSession(sessionId) }.onSuccess { applySession(it) }
            _uiState.value = _uiState.value.copy(isSending = false)
        }
    }

    fun completeTriage() {
        val sessionId = _uiState.value.sessionId ?: return
        viewModelScope.launch {
            runCatching { triageRepository.completeSession(sessionId) }
                .onSuccess { completed ->
                    sessionManager.saveTriageSessionId(completed.resolvedId)
                    _uiState.value = _uiState.value.copy(completedSessionId = completed.resolvedId)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(completedSessionId = sessionId)
                }
        }
    }
}
