package com.vitals.mobile.feature.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.medicalrecords.MedicalRecordsRepository
import com.vitals.mobile.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DocumentNewUiState(
    val title: String = "",
    val docType: String = "",
    val date: String = "",
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class DocumentNewViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val medicalRecordsRepository: MedicalRecordsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentNewUiState())
    val uiState = _uiState.asStateFlow()

    fun updateTitle(value: String) {
        _uiState.value = _uiState.value.copy(title = value)
    }

    fun updateType(value: String) {
        _uiState.value = _uiState.value.copy(docType = value)
    }

    fun updateDate(value: String) {
        _uiState.value = _uiState.value.copy(date = value)
    }

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Введите название документа")
            return
        }
        _uiState.value = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            val patientId = sessionManager.currentSession().patientId
            if (patientId == null) {
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = "Профиль пациента не найден")
                return@launch
            }
            runCatching {
                medicalRecordsRepository.recordDocument(
                    patientId = patientId,
                    title = state.title,
                    docType = state.docType.ifBlank { "Документ" },
                    hasPaper = false,
                    hasDigital = true,
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = error.message ?: "Не удалось сохранить документ")
            }
        }
    }
}
