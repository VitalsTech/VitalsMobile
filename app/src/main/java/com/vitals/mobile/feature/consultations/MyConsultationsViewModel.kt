package com.vitals.mobile.feature.consultations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.common.ConsultationLabels
import com.vitals.mobile.core.data.consultations.ConsultationDto
import com.vitals.mobile.core.data.consultations.ConsultationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyConsultationsUiState(
    val isLoading: Boolean = true,
    val scheduled: List<ConsultationDto> = emptyList(),
    val chats: List<ConsultationDto> = emptyList(),
    val completed: List<ConsultationDto> = emptyList(),
)

fun typeLabel(consultationType: String?): String = ConsultationLabels.type(consultationType)

@HiltViewModel
class MyConsultationsViewModel @Inject constructor(
    private val consultationsRepository: ConsultationsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyConsultationsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            runCatching { consultationsRepository.mine(includeCompleted = true, limit = 50) }
                .onSuccess { list ->
                    val sorted = list.sortedByDescending { activityKey(it) }
                    val active = sorted.filterNot { ConsultationLabels.isTerminal(it.status) }
                    val done = sorted.filter { ConsultationLabels.isTerminal(it.status) }
                    val scheduled = active.filter { it.isSlotBooking() }
                    val chats = active.filterNot { it.isSlotBooking() }
                    _uiState.value = MyConsultationsUiState(
                        isLoading = false,
                        scheduled = scheduled,
                        chats = chats,
                        completed = done,
                    )
                }
                .onFailure { _uiState.value = MyConsultationsUiState(isLoading = false) }
        }
    }

    private fun activityKey(c: ConsultationDto): String =
        c.scheduledAt ?: c.lastActivityAt ?: c.completedAt ?: c.createdAt.orEmpty()
}
