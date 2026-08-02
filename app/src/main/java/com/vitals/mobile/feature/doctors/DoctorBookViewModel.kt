package com.vitals.mobile.feature.doctors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.common.ScheduleSlotLabels
import com.vitals.mobile.core.data.consultations.ConsultationType
import com.vitals.mobile.core.data.consultations.ConsultationsRepository
import com.vitals.mobile.core.data.doctors.DoctorsRepository
import com.vitals.mobile.core.data.doctors.ScheduleSlotDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DoctorBookUiState(
    val isLoading: Boolean = true,
    val availableDates: List<LocalDate> = emptyList(),
    val selectedDate: LocalDate? = null,
    val slotsForDate: List<ScheduleSlotDto> = emptyList(),
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

    private var allSlots: List<ScheduleSlotDto> = emptyList()

    fun load(doctorId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val from = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            runCatching { doctorsRepository.getSchedule(doctorId, from = from, days = 14) }
                .onSuccess { raw ->
                    allSlots = raw
                        .filter { ScheduleSlotLabels.isAvailable(it) }
                        .filter { ScheduleSlotLabels.isInFuture(it) || ScheduleSlotLabels.startIso(it).isBlank() }
                        .sortedBy { ScheduleSlotLabels.startIso(it) }

                    val dates = allSlots.map { slotDate(it) }.distinct().sorted()
                    val selectedDate = dates.firstOrNull()
                    applyDate(selectedDate, clearErrorIfEmpty = dates.isEmpty())
                }
                .onFailure { error ->
                    allSlots = emptyList()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        availableDates = emptyList(),
                        selectedDate = null,
                        slotsForDate = emptyList(),
                        selectedSlotId = null,
                        errorMessage = error.message ?: "Не удалось загрузить слоты",
                    )
                }
        }
    }

    fun selectDate(date: LocalDate) {
        applyDate(date)
    }

    fun selectSlot(slotId: String) {
        _uiState.value = _uiState.value.copy(selectedSlotId = slotId)
    }

    fun selectType(type: ConsultationType) {
        _uiState.value = _uiState.value.copy(consultationType = type)
    }

    private fun slotDate(slot: ScheduleSlotDto): LocalDate =
        ScheduleSlotLabels.localDate(slot) ?: LocalDate.now()

    private fun applyDate(date: LocalDate?, clearErrorIfEmpty: Boolean = false) {
        val forDate = if (date == null) emptyList() else allSlots.filter { slotDate(it) == date }
        val dates = allSlots.map { slotDate(it) }.distinct().sorted()
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            availableDates = dates,
            selectedDate = date,
            slotsForDate = forDate,
            selectedSlotId = forDate.firstOrNull()?.resolvedId,
            errorMessage = when {
                dates.isEmpty() || clearErrorIfEmpty -> "Нет доступных слотов на ближайшие дни"
                forDate.isEmpty() -> "Нет слотов на выбранную дату"
                else -> null
            },
        )
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
