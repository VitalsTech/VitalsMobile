package com.vitals.mobile.feature.consultations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
)

fun typeLabel(consultationType: String?): String = when (consultationType) {
    "InPerson" -> "Очный приём"
    "Video" -> "Онлайн"
    "SyncChat" -> "Консультация"
    "Async" -> "Уточнение"
    "HomeVisit" -> "Вызов на дом"
    else -> "Консультация"
}

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
            runCatching { consultationsRepository.mine() }
                .onSuccess { list ->
                    val (scheduled, chats) = list.partition { it.scheduledAt != null }
                    _uiState.value = MyConsultationsUiState(isLoading = false, scheduled = scheduled, chats = chats)
                }
                .onFailure { _uiState.value = MyConsultationsUiState(isLoading = false) }
        }
    }
}
