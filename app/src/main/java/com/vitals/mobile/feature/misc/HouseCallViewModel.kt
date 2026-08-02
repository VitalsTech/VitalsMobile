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

data class HouseCallUiState(
    val address: String = "",
    val symptoms: String = "",
    val isSubmitting: Boolean = false,
    val submitted: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class HouseCallViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val medicalRecordsRepository: MedicalRecordsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HouseCallUiState())
    val uiState = _uiState.asStateFlow()

    fun updateAddress(value: String) {
        _uiState.value = _uiState.value.copy(address = value)
    }

    fun updateSymptoms(value: String) {
        _uiState.value = _uiState.value.copy(symptoms = value)
    }

    fun submit() {
        val state = _uiState.value
        if (state.address.isBlank() || state.symptoms.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Заполните адрес и симптомы")
            return
        }
        _uiState.value = state.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            val patientId = sessionManager.currentSession().patientId
            if (patientId == null) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, errorMessage = "Профиль пациента не найден")
                return@launch
            }
            runCatching {
                medicalRecordsRepository.recordHouseCallRequest(
                    patientId = patientId,
                    address = state.address,
                    symptoms = state.symptoms,
                    desiredTime = null,
                    phone = null,
                    urgent = false,
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isSubmitting = false, submitted = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(isSubmitting = false, errorMessage = error.message ?: "Не удалось отправить заявку")
            }
        }
    }
}
