package com.vitals.mobile.feature.treatment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.common.LabLabels
import com.vitals.mobile.core.data.common.RouteLabels
import com.vitals.mobile.core.data.medicalrecords.MedicalRecordsRepository
import com.vitals.mobile.core.data.prescriptions.PrescriptionsRepository
import com.vitals.mobile.core.data.routing.RoutingRepository
import com.vitals.mobile.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TreatmentUiState(
    val isLoading: Boolean = true,
    val done: List<String> = emptyList(),
    val pending: List<String> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class TreatmentViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val routingRepository: RoutingRepository,
    private val medicalRecordsRepository: MedicalRecordsRepository,
    private val prescriptionsRepository: PrescriptionsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TreatmentUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val session = sessionManager.currentSession()
            val patientId = session.patientId
            val aliases = listOfNotNull(patientId, session.publicId).distinct()
            if (patientId == null) {
                _uiState.value = TreatmentUiState(isLoading = false, errorMessage = "Профиль пациента не найден")
                return@launch
            }

            val route = runCatching { routingRepository.getActiveRoute(patientId) }.getOrNull()
            val history = runCatching { medicalRecordsRepository.getHistoryAliases(aliases) }.getOrElse { emptyList() }
            val prescriptions = runCatching { prescriptionsRepository.getForPatientAliases(aliases) }
                .getOrElse { emptyList() }

            val steps = route?.steps.orEmpty()
                .sortedBy { it.stepNumber ?: it.order ?: Int.MAX_VALUE }

            val doneFromRoute = steps
                .filter { RouteLabels.isStepDone(it.status) }
                .mapIndexed { index, step -> RouteLabels.stepTitle(step, index) }

            val pendingFromRoute = steps
                .filterNot { RouteLabels.isStepDone(it.status) }
                .mapIndexed { index, step -> RouteLabels.stepTitle(step, index) }

            val historyTitles = history.mapNotNull { event ->
                val type = event.eventType?.trim().orEmpty()
                when {
                    type.equals("diagnosis", ignoreCase = true) ||
                        type.equals("DiagnosisConfirmed", ignoreCase = true) ->
                        medicalRecordsRepository.diagnosisLabelFromEvent(event)
                            ?: "Диагноз зафиксирован"
                    type.equals("PrescriptionIssued", ignoreCase = true) ||
                        type.equals("prescription", ignoreCase = true) -> "Выписан рецепт"
                    type.contains("triage", ignoreCase = true) -> "Пройден ИИ-триаж"
                    else -> null
                }
            }.distinct()

            val openRx = prescriptions
                .filter {
                    val s = it.status?.lowercase().orEmpty()
                    s !in setOf("fulfilled", "dispensed", "cancelled", "canceled", "expired")
                }
                .map { rx ->
                    "${rx.formatMedications()} - ${LabLabels.prescriptionStatus(rx.status)}"
                }

            val done = (doneFromRoute + historyTitles).distinct()
            val pending = (pendingFromRoute + openRx).distinct()

            _uiState.value = TreatmentUiState(
                isLoading = false,
                done = done,
                pending = pending,
                errorMessage = if (route == null && history.isEmpty() && prescriptions.isEmpty()) {
                    "Не удалось загрузить данные лечения"
                } else {
                    null
                },
            )
        }
    }
}
