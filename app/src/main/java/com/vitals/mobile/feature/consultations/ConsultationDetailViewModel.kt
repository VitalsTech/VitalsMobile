package com.vitals.mobile.feature.consultations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.consultations.ConsultationDto
import com.vitals.mobile.core.data.consultations.ConsultationsRepository
import com.vitals.mobile.feature.common.UiChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConsultationDetailUiState(
    val isLoading: Boolean = true,
    val consultation: ConsultationDto? = null,
    val messages: List<UiChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
) {
    val isCompleted: Boolean get() = consultation?.status?.equals("Completed", ignoreCase = true) == true
    val hasProtocol: Boolean get() = consultation?.protocol != null
}

@HiltViewModel
class ConsultationDetailViewModel @Inject constructor(
    private val consultationsRepository: ConsultationsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsultationDetailUiState())
    val uiState = _uiState.asStateFlow()

    fun load(sessionId: String) {
        viewModelScope.launch {
            runCatching { consultationsRepository.get(sessionId) }
                .onSuccess { consultation -> _uiState.value = _uiState.value.copy(consultation = consultation) }
                .onFailure { }
            runCatching { consultationsRepository.getMessages(sessionId) }
                .onSuccess { messages ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        messages = messages.map { UiChatMessage(id = it.resolvedId, text = it.resolvedText, fromMe = it.isFromCurrentUser) },
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false) }
        }
    }

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendCurrentInput(sessionId: String) {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isCompleted) return
        val optimistic = UiChatMessage(id = "local-${System.nanoTime()}", text = text, fromMe = true)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + optimistic,
            inputText = "",
            isSending = true,
        )
        viewModelScope.launch {
            runCatching { consultationsRepository.sendMessage(sessionId, text) }
            _uiState.value = _uiState.value.copy(isSending = false)
        }
    }
}
