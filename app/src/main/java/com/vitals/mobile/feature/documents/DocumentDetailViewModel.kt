package com.vitals.mobile.feature.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.medicalrecords.DocumentEventPayload
import com.vitals.mobile.core.data.medicalrecords.MedicalRecordEventTypes
import com.vitals.mobile.core.data.medicalrecords.MedicalRecordsRepository
import com.vitals.mobile.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class DocumentDetailUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val note: String = "",
    val errorMessage: String? = null,
)

@HiltViewModel
class DocumentDetailViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val medicalRecordsRepository: MedicalRecordsRepository,
    private val json: Json,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentDetailUiState())
    val uiState = _uiState.asStateFlow()

    fun load(documentId: String) {
        viewModelScope.launch {
            val patientId = sessionManager.currentSession().patientId
            if (patientId == null) {
                _uiState.value = DocumentDetailUiState(isLoading = false, errorMessage = "Профиль пациента не найден")
                return@launch
            }
            runCatching { medicalRecordsRepository.getHistory(patientId, listOf(MedicalRecordEventTypes.DOCUMENT)) }
                .onSuccess { events ->
                    val event = events.firstOrNull { it.id == documentId }
                    val payload = event?.payloadJson?.let { raw ->
                        runCatching { json.decodeFromString(DocumentEventPayload.serializer(), raw) }.getOrNull()
                    }
                    if (payload == null) {
                        _uiState.value = DocumentDetailUiState(isLoading = false, errorMessage = "Документ не найден")
                    } else {
                        _uiState.value = DocumentDetailUiState(
                            isLoading = false,
                            title = payload.title,
                            note = payload.docType,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = DocumentDetailUiState(
                        isLoading = false,
                        errorMessage = error.message ?: "Не удалось загрузить документ",
                    )
                }
        }
    }
}
