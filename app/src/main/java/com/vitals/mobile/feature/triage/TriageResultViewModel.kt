package com.vitals.mobile.feature.triage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.triage.TriageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TriageResultUiState(
    val isLoading: Boolean = true,
    val urgencyTitle: String = "",
    val urgencySubtitle: String = "",
    val routeSteps: List<String> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class TriageResultViewModel @Inject constructor(
    private val triageRepository: TriageRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TriageResultUiState())
    val uiState = _uiState.asStateFlow()

    fun load(sessionId: String) {
        viewModelScope.launch {
            runCatching { triageRepository.getSession(sessionId) }
                .onSuccess { session ->
                    val doctor = session.assignedDoctorName
                    val hypotheses = session.hypotheses.orEmpty()
                    val labs = session.recommendedLabs.orEmpty()

                    val steps = buildList {
                        if (!doctor.isNullOrBlank()) add("Запись к врачу: $doctor")
                        labs.forEach { add("Анализ: $it") }
                    }

                    _uiState.value = TriageResultUiState(
                        isLoading = false,
                        urgencyTitle = session.urgencyLevel.orEmpty().ifBlank { "Результат триажа" },
                        urgencySubtitle = hypotheses.joinToString(", "),
                        routeSteps = steps,
                    )
                }
                .onFailure { error ->
                    _uiState.value = TriageResultUiState(
                        isLoading = false,
                        errorMessage = error.message ?: "Не удалось загрузить результат",
                    )
                }
        }
    }
}
