package com.vitals.mobile.feature.consultations

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.common.ConsultationLabels
import com.vitals.mobile.core.data.consultations.ConsultationDto
import com.vitals.mobile.core.data.consultations.ConsultationHubClient
import com.vitals.mobile.core.data.consultations.ConsultationsRepository
import com.vitals.mobile.core.session.SessionManager
import com.vitals.mobile.feature.common.UiChatMessage
import com.vitals.mobile.feature.common.optimisticUiChatMessage
import com.vitals.mobile.feature.common.toUiChatMessage
import com.vitals.mobile.feature.consultations.video.VideoCallCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class ConsultationDetailUiState(
    val isLoading: Boolean = true,
    val consultation: ConsultationDto? = null,
    val messages: List<UiChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
) {
    val isCompleted: Boolean get() = ConsultationLabels.isTerminal(consultation?.status)
    val hasProtocol: Boolean get() = consultation?.protocol != null
}

@HiltViewModel
class ConsultationDetailViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    private val consultationsRepository: ConsultationsRepository,
    sessionManager: SessionManager,
    json: Json,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsultationDetailUiState())
    val uiState = _uiState.asStateFlow()

    val video = VideoCallCoordinator(
        appContext = appContext,
        repository = consultationsRepository,
        hub = ConsultationHubClient(sessionManager, json),
        scope = viewModelScope,
    )

    private var pollJob: Job? = null
    private var activeSessionId: String? = null

    init {
        viewModelScope.launch {
            video.refreshChat.collect {
                val id = activeSessionId ?: return@collect
                refreshMessages(id)
            }
        }
        viewModelScope.launch {
            video.statusChanges.collect { status ->
                val current = _uiState.value.consultation ?: return@collect
                _uiState.value = _uiState.value.copy(consultation = current.copy(status = status))
            }
        }
    }

    fun load(sessionId: String) {
        pollJob?.cancel()
        activeSessionId = sessionId
        viewModelScope.launch {
            runCatching { consultationsRepository.get(sessionId) }
                .onSuccess { consultation ->
                    _uiState.value = _uiState.value.copy(consultation = consultation)
                    runCatching { consultationsRepository.join(sessionId) }
                    val terminal = ConsultationLabels.isTerminal(consultation.status)
                    video.attach(sessionId, enabled = !terminal)
                }
                .onFailure { }
            refreshMessages(sessionId, setLoadingFalse = true)
            startPolling(sessionId)
        }
    }

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendCurrentInput(sessionId: String) {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isCompleted) return
        val optimistic = optimisticUiChatMessage(text)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + optimistic,
            inputText = "",
            isSending = true,
        )
        viewModelScope.launch {
            runCatching { consultationsRepository.sendMessage(sessionId, text) }
            refreshMessages(sessionId)
            _uiState.value = _uiState.value.copy(isSending = false)
        }
    }

    private fun startPolling(sessionId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                if (_uiState.value.isSending) continue
                refreshMessages(sessionId)
            }
        }
    }

    private suspend fun refreshMessages(sessionId: String, setLoadingFalse: Boolean = false) {
        runCatching { consultationsRepository.getMessages(sessionId) }
            .onSuccess { messages ->
                val mapped = messages.map { it.toUiChatMessage() }
                val current = _uiState.value
                if (mapped != current.messages || (setLoadingFalse && current.isLoading)) {
                    _uiState.value = current.copy(
                        isLoading = if (setLoadingFalse) false else current.isLoading,
                        messages = mapped,
                    )
                }
            }
            .onFailure {
                if (setLoadingFalse) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
    }

    override fun onCleared() {
        pollJob?.cancel()
        video.release()
        super.onCleared()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 2_000L
    }
}
