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

data class DocumentUi(
    val id: String,
    val title: String,
    val docType: String,
    val date: String,
)

data class DocumentsUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val documents: List<DocumentUi> = emptyList(),
    val errorMessage: String? = null,
) {
    val filtered: List<DocumentUi>
        get() = if (query.isBlank()) documents else documents.filter { it.title.contains(query, ignoreCase = true) }
}

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val medicalRecordsRepository: MedicalRecordsRepository,
    private val json: Json,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun load() {
        viewModelScope.launch {
            val patientId = sessionManager.currentSession().patientId
            if (patientId == null) {
                _uiState.value = DocumentsUiState(isLoading = false, errorMessage = "Профиль пациента не найден")
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { medicalRecordsRepository.getHistory(patientId, listOf(MedicalRecordEventTypes.DOCUMENT)) }
                .onSuccess { events ->
                    val docs = events.mapNotNull { event ->
                        val payload = event.payloadJson?.let { raw ->
                            runCatching { json.decodeFromString(DocumentEventPayload.serializer(), raw) }.getOrNull()
                        }
                        payload?.let {
                            DocumentUi(
                                id = event.id ?: event.effectiveDate,
                                title = it.title,
                                docType = it.docType,
                                date = event.effectiveDate,
                            )
                        }
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, documents = docs)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        documents = emptyList(),
                        errorMessage = error.message ?: "Не удалось загрузить документы",
                    )
                }
        }
    }
}
