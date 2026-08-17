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
    val isStartingNew: Boolean = false,
    val readyToComplete: Boolean = false,
    val isSessionCompleted: Boolean = false,
    val completeSuggestion: String? = null,
    /** Set only after the user completes triage in this session - triggers navigation to result. */
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

    /** Called when the ИИ tab becomes visible - honours pending «новый триаж» from Path / result. */
    fun onScreenVisible() {
        viewModelScope.launch {
            if (sessionManager.consumePendingNewTriage()) {
                createFreshSession()
            }
        }
    }

    /** Like web Triage `startNew`: drop current session and open a new empty triage chat. */
    fun startNewTriage() {
        if (_uiState.value.isStartingNew || _uiState.value.isLoading) return
        viewModelScope.launch {
            createFreshSession()
        }
    }

    private fun resumeOrCreateSession() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, completedSessionId = null)
            if (sessionManager.consumePendingNewTriage()) {
                createFreshSession()
                return@launch
            }

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
                    // Completed sessions stay on this screen (with «Новый триаж»), like web -
                    // do not auto-navigate to result on resume.
                    applySession(resumed, navigateToResult = false)
                    return@launch
                }
            }

            createFreshSession(patientId)
        }
    }

    private suspend fun createFreshSession(patientIdOverride: String? = null) {
        _uiState.value = AiAssistantUiState(isLoading = true, isStartingNew = true)
        sessionManager.saveTriageSessionId(null)

        val patientId = patientIdOverride ?: sessionManager.currentSession().patientId
        if (patientId == null) {
            _uiState.value = AiAssistantUiState(
                isLoading = false,
                errorMessage = "Профиль пациента не найден",
            )
            return
        }

        runCatching { triageRepository.createSession(patientId) }
            .onSuccess { created ->
                sessionManager.saveTriageSessionId(created.resolvedId)
                applySession(created, navigateToResult = false)
            }
            .onFailure { error ->
                _uiState.value = AiAssistantUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Не удалось начать новый триаж",
                )
            }
    }

    private fun applySession(session: TriageSessionDto, navigateToResult: Boolean) {
        val remoteMessages = session.messages.orEmpty().map { it.toUiChatMessage() }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isStartingNew = false,
            sessionId = session.resolvedId,
            messages = remoteMessages,
            readyToComplete = session.resolvedReadyToComplete || session.isCompleted,
            isSessionCompleted = session.isCompleted,
            completeSuggestion = session.resolvedCompleteSuggestion,
            errorMessage = null,
            completedSessionId = if (navigateToResult && session.isCompleted) session.resolvedId else null,
            inputText = "",
            isSending = false,
            isCompleting = false,
        )
    }

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendCurrentInput() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return
        if (_uiState.value.isSessionCompleted) return
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
                .onSuccess { applySession(it, navigateToResult = false) }
            _uiState.value = _uiState.value.copy(isSending = false)
        }
    }

    fun completeTriage() {
        val sessionId = _uiState.value.sessionId ?: return
        if (_uiState.value.isCompleting || _uiState.value.isSessionCompleted) return
        _uiState.value = _uiState.value.copy(isCompleting = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { triageRepository.completeSession(sessionId) }
                .onSuccess { completed ->
                    sessionManager.saveTriageSessionId(completed.resolvedId)
                    _uiState.value = _uiState.value.copy(
                        isCompleting = false,
                        readyToComplete = true,
                        isSessionCompleted = true,
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
