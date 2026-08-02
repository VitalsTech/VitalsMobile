package com.vitals.mobile.feature.misc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.medicalrecords.MedicalRecordsRepository
import com.vitals.mobile.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

val SUPPORT_FAQ = listOf(
    "Как записаться?",
    "Как добавить документ?",
    "Как работает триаж?",
)

data class SupportUiState(
    val question: String = "",
    val isSending: Boolean = false,
    val sentMessage: String? = null,
)

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val medicalRecordsRepository: MedicalRecordsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupportUiState())
    val uiState = _uiState.asStateFlow()

    fun updateQuestion(value: String) {
        _uiState.value = _uiState.value.copy(question = value)
    }

    fun selectFaq(topic: String) {
        _uiState.value = _uiState.value.copy(question = topic)
    }

    fun send() {
        val question = _uiState.value.question.trim()
        if (question.isEmpty()) return
        _uiState.value = _uiState.value.copy(isSending = true)
        viewModelScope.launch {
            val patientId = sessionManager.currentSession().patientId
            if (patientId != null) {
                runCatching { medicalRecordsRepository.recordSupportRequest(patientId, topic = "faq", message = question) }
            }
            _uiState.value = SupportUiState(sentMessage = "Вопрос отправлен в поддержку")
        }
    }
}
