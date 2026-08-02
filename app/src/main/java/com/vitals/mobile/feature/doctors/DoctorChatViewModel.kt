package com.vitals.mobile.feature.doctors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.consultations.ConsultationsRepository
import com.vitals.mobile.feature.common.UiChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DoctorChatUiState(
    val isLoading: Boolean = true,
    val consultationId: String? = null,
    val messages: List<UiChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
)

@HiltViewModel
class DoctorChatViewModel @Inject constructor(
    private val consultationsRepository: ConsultationsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorChatUiState())
    val uiState = _uiState.asStateFlow()

    fun load(doctorId: String) {
        viewModelScope.launch {
            val consultations = runCatching { consultationsRepository.mine() }.getOrElse { emptyList() }
            val active = consultations.firstOrNull { it.doctorId == doctorId }
            if (active == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    messages = listOf(UiChatMessage(id = "empty", text = "У вас пока нет активной консультации с этим врачом.", fromMe = false)),
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(consultationId = active.resolvedId)
            runCatching { consultationsRepository.getMessages(active.resolvedId) }
                .onSuccess { messages ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        messages = messages.map {
                            UiChatMessage(id = it.resolvedId, text = it.resolvedText, fromMe = it.isFromCurrentUser)
                        },
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false) }
        }
    }

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendCurrentInput() {
        val text = _uiState.value.inputText.trim()
        val consultationId = _uiState.value.consultationId ?: return
        if (text.isEmpty()) return
        val optimistic = UiChatMessage(id = "local-${System.nanoTime()}", text = text, fromMe = true)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + optimistic,
            inputText = "",
            isSending = true,
        )
        viewModelScope.launch {
            runCatching { consultationsRepository.sendMessage(consultationId, text) }
            _uiState.value = _uiState.value.copy(isSending = false)
        }
    }
}
