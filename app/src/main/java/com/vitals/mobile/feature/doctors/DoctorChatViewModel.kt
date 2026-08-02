package com.vitals.mobile.feature.doctors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private var pollJob: Job? = null

    fun load(doctorId: String) {
        pollJob?.cancel()
        viewModelScope.launch {
            val consultations = runCatching { consultationsRepository.mine() }.getOrElse { emptyList() }
            val active = consultations.firstOrNull { it.doctorId == doctorId }
            if (active == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    consultationId = null,
                    messages = listOf(
                        UiChatMessage(
                            id = "empty",
                            text = "У вас пока нет активной консультации с этим врачом.",
                            fromMe = false,
                        ),
                    ),
                )
                return@launch
            }
            val consultationId = active.resolvedId
            _uiState.value = _uiState.value.copy(consultationId = consultationId)
            refreshMessages(consultationId, setLoadingFalse = true)
            startPolling(consultationId)
        }
    }

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendCurrentInput() {
        val text = _uiState.value.inputText.trim()
        val consultationId = _uiState.value.consultationId ?: return
        if (text.isEmpty()) return
        val optimistic = optimisticUiChatMessage(text)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + optimistic,
            inputText = "",
            isSending = true,
        )
        viewModelScope.launch {
            runCatching { consultationsRepository.sendMessage(consultationId, text) }
            refreshMessages(consultationId)
            _uiState.value = _uiState.value.copy(isSending = false)
        }
    }

    private fun startPolling(consultationId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                if (_uiState.value.isSending) continue
                refreshMessages(consultationId)
            }
        }
    }

    private suspend fun refreshMessages(consultationId: String, setLoadingFalse: Boolean = false) {
        runCatching { consultationsRepository.getMessages(consultationId) }
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
