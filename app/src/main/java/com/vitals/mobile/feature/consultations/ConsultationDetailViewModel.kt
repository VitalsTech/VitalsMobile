package com.vitals.mobile.feature.consultations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.common.ConsultationLabels
import com.vitals.mobile.core.data.consultations.ConsultationDto
import com.vitals.mobile.core.data.consultations.ConsultationsRepository
import com.vitals.mobile.feature.common.UiChatMessage
import com.vitals.mobile.feature.common.optimisticUiChatMessage
import com.vitals.mobile.feature.common.toUiChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    private val consultationsRepository: ConsultationsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsultationDetailUiState())
    val uiState = _uiState.asStateFlow()

    private var pollJob: Job? = null

    fun load(sessionId: String) {
        pollJob?.cancel()
        viewModelScope.launch {
            runCatching { consultationsRepository.get(sessionId) }
                .onSuccess { consultation -> _uiState.value = _uiState.value.copy(consultation = consultation) }
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
        super.onCleared()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 2_000L
    }
}
