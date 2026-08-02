package com.vitals.mobile.feature.labs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.common.LabLabels
import com.vitals.mobile.core.data.common.RouteLabels
import com.vitals.mobile.core.data.consultations.ConsultationsRepository
import com.vitals.mobile.core.data.laborders.LabOrdersRepository
import com.vitals.mobile.core.data.medicalrecords.MedicalRecordsRepository
import com.vitals.mobile.core.data.prescriptions.PrescriptionsRepository
import com.vitals.mobile.core.data.routing.RoutingRepository
import com.vitals.mobile.core.data.routing.RoutingStepDto
import com.vitals.mobile.core.data.triage.TriageRepository
import com.vitals.mobile.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LabLineUi(
    val title: String,
    val status: String,
)

data class PrescriptionLineUi(
    val id: String,
    val title: String,
    val status: String,
    val subtitle: String = "",
    val qrAvailable: Boolean = false,
)

data class LabsUiState(
    val isLoading: Boolean = true,
    val labLines: List<LabLineUi> = emptyList(),
    val prescriptionLines: List<PrescriptionLineUi> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class LabsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val labOrdersRepository: LabOrdersRepository,
    private val prescriptionsRepository: PrescriptionsRepository,
    private val routingRepository: RoutingRepository,
    private val consultationsRepository: ConsultationsRepository,
    private val triageRepository: TriageRepository,
    private val medicalRecordsRepository: MedicalRecordsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LabsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val session = sessionManager.currentSession()
            val patientId = session.patientId
            val aliases = listOfNotNull(patientId, session.publicId).distinct()
            if (aliases.isEmpty()) {
                _uiState.value = LabsUiState(isLoading = false, errorMessage = "Профиль пациента не найден")
                return@launch
            }

            val labOrders = runCatching { labOrdersRepository.getForPatientAliases(aliases) }
                .getOrElse { emptyList() }
            val prescriptions = runCatching { prescriptionsRepository.getForPatientAliases(aliases) }
                .getOrElse { emptyList() }
            val route = patientId?.let { runCatching { routingRepository.getActiveRoute(it) }.getOrNull() }
            val mine = runCatching { consultationsRepository.mine(includeCompleted = true, limit = 50) }
                .getOrElse { emptyList() }
            val triageSession = session.triageSessionId?.let {
                runCatching { triageRepository.getSession(it) }.getOrNull()
            }
            val mrState = runCatching { medicalRecordsRepository.getStateAliases(aliases) }.getOrNull()

            val recommended = buildList {
                addAll(route?.recommendedLabs.orEmpty())
                val decisionId = route?.resolvedDecisionId
                if (!decisionId.isNullOrBlank()) {
                    val decision = runCatching { routingRepository.getDecision(decisionId) }.getOrNull()
                    addAll(decision?.recommendedLabs.orEmpty())
                }
                addAll(triageSession?.recommendedLabs.orEmpty())
                addAll(labsFromRouteSteps(route?.steps.orEmpty()))
            }.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

            val protocolLabs = mine.flatMap { c ->
                c.protocol?.labOrders.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }
            }.distinct()

            // Как anamnesis.recentLabResults у врача: "hjkl;: Назначено"
            val anamnesisLabs = mrState?.recentLabResults.orEmpty()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { raw ->
                    val name = raw.substringBefore(':').trim().ifBlank { raw }
                    val status = raw.substringAfter(':', missingDelimiterValue = "Назначено").trim()
                        .ifBlank { "Назначено" }
                    name to status
                }

            val orderedNames = buildSet {
                labOrders.forEach { order ->
                    add(order.formatTestNames().trim().lowercase())
                    (order.items ?: order.tests).orEmpty().forEach { item ->
                        add(item.resolvedName.trim().lowercase())
                    }
                }
            }

            val lines = buildList {
                labOrders.forEach { order ->
                    add(
                        LabLineUi(
                            title = order.formatTestNames(),
                            status = LabLabels.orderStatus(order.status),
                        ),
                    )
                }
                // Как web Labs: если формальных направлений нет — показываем из протокола.
                if (labOrders.isEmpty()) {
                    protocolLabs.forEach { name ->
                        add(LabLineUi(title = name, status = "Из протокола"))
                    }
                } else {
                    protocolLabs
                        .filter { it.lowercase() !in orderedNames }
                        .forEach { name ->
                            add(LabLineUi(title = name, status = "Из протокола"))
                        }
                }
                val shown = map { it.title.trim().lowercase() }.toMutableSet()
                anamnesisLabs
                    .filter { (name, _) -> name.lowercase() !in orderedNames && name.lowercase() !in shown }
                    .forEach { (name, status) ->
                        shown += name.lowercase()
                        add(LabLineUi(title = name, status = status))
                    }
                recommended
                    .filter { it.lowercase() !in orderedNames && it.lowercase() !in shown }
                    .forEach { name ->
                        shown += name.lowercase()
                        add(LabLineUi(title = name, status = "Рекомендовано"))
                    }
            }

            val rxLines = prescriptions.map { prescription ->
                val med = prescription.medications?.firstOrNull()
                val scheme = listOfNotNull(med?.dosage, med?.frequency)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(" · ")
                PrescriptionLineUi(
                    id = prescription.resolvedId,
                    title = prescription.displayTitle(),
                    status = LabLabels.prescriptionStatus(prescription.status),
                    subtitle = scheme,
                    qrAvailable = prescription.canShowQr(),
                )
            }

            _uiState.value = LabsUiState(
                isLoading = false,
                labLines = lines,
                prescriptionLines = rxLines,
            )
        }
    }

    private fun labsFromRouteSteps(steps: List<RoutingStepDto>): List<String> {
        for (step in steps) {
            if (!RouteLabels.isLabAction(step)) continue
            val description = step.description.orEmpty()
            val match = Regex("""анализы:\s*(.+)$""", RegexOption.IGNORE_CASE).find(description)
            if (match != null) {
                return match.groupValues[1]
                    .split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }
        }
        return emptyList()
    }
}
