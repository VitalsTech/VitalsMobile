package com.vitals.mobile.feature.doctors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.consultations.ConsultationType
import com.vitals.mobile.core.data.consultations.ConsultationsRepository
import com.vitals.mobile.core.data.doctors.DoctorsRepository
import com.vitals.mobile.core.data.doctors.ScheduleSlotDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DoctorBookUiState(
    val isLoading: Boolean = true,
    val slots: List<ScheduleSlotDto> = emptyList(),
    val selectedSlotId: String? = null,
    val consultationType: ConsultationType = ConsultationType.IN_PERSON,
    val isSubmitting: Boolean = false,
    val bookedSessionId: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class DoctorBookViewModel @Inject constructor(
    private val doctorsRepository: DoctorsRepository,
    private val consultationsRepository: ConsultationsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorBookUiState())
    val uiState = _uiState.asStateFlow()

    fun load(doctorId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { doctorsRepository.getSchedule(doctorId) }
                .onSuccess { slots ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        slots = slots,
                        selectedSlotId = slots.firstOrNull()?.resolvedId,
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        slots = emptyList(),
                        errorMessage = error.message ?: "Не удалось загрузить слоты",
                    )
                }
        }
    }

    fun selectSlot(slotId: String) {
        _uiState.value = _uiState.value.copy(selectedSlotId = slotId)
    }

    fun selectType(type: ConsultationType) {
        _uiState.value = _uiState.value.copy(consultationType = type)
    }

    fun confirm(doctorId: String) {
        val state = _uiState.value
        val slotId = state.selectedSlotId
        if (slotId.isNullOrBlank()) {
            _uiState.value = state.copy(errorMessage = "Выберите время приёма")
            return
        }
        _uiState.value = state.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                consultationsRepository.book(
                    doctorId = doctorId,
                    slotId = slotId,
                    consultationType = state.consultationType,
                )
            }.onSuccess { consultation ->
                _uiState.value = _uiState.value.copy(isSubmitting = false, bookedSessionId = consultation.resolvedId)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = error.message ?: "Не удалось записаться",
                )
            }
        }
    }
}
