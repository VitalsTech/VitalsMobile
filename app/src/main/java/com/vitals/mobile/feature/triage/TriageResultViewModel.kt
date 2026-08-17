package com.vitals.mobile.feature.triage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.common.RouteLabels
import com.vitals.mobile.core.data.routing.RoutingRepository
import com.vitals.mobile.core.data.triage.TriageRepository
import com.vitals.mobile.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TriageResultUiState(
    val isLoading: Boolean = true,
    val urgencyTitle: String = "",
    val urgencySubtitle: String = "",
    val recommendation: String = "",
    val routeSteps: List<String> = emptyList(),
    val assignedDoctorId: String? = null,
    val assignedDoctorName: String? = null,
    val consultationSessionId: String? = null,
    val recommendedLabs: List<String> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class TriageResultViewModel @Inject constructor(
    private val triageRepository: TriageRepository,
    private val routingRepository: RoutingRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TriageResultUiState())
    val uiState = _uiState.asStateFlow()

    fun load(sessionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val session = runCatching { triageRepository.getSession(sessionId) }.getOrNull()
            if (session == null) {
                _uiState.value = TriageResultUiState(
                    isLoading = false,
                    errorMessage = "Не удалось загрузить результат триажа",
                )
                return@launch
            }

            val patientId = sessionManager.currentSession().patientId
            var routeSteps = emptyList<String>()
            var labs = session.recommendedLabs.orEmpty().filter { it.isNotBlank() }

            if (patientId != null) {
                // Routing may land slightly after complete - brief retry like web.
                repeat(3) { attempt ->
                    val route = runCatching { routingRepository.getActiveRoute(patientId) }.getOrNull()
                    val steps = route?.steps.orEmpty()
                        .sortedBy { it.stepNumber ?: it.order ?: Int.MAX_VALUE }
                    if (steps.isNotEmpty()) {
                        routeSteps = steps.mapIndexed { index, step -> RouteLabels.stepTitle(step, index) }
                    }
                    if (labs.isEmpty()) {
                        labs = route?.recommendedLabs.orEmpty().filter { it.isNotBlank() }
                    }
                    val decisionId = route?.resolvedDecisionId ?: session.routingDecisionId
                    if (labs.isEmpty() && !decisionId.isNullOrBlank()) {
                        val decision = runCatching { routingRepository.getDecision(decisionId) }.getOrNull()
                        labs = decision?.recommendedLabs.orEmpty().filter { it.isNotBlank() }
                    }
                    if (routeSteps.isNotEmpty()) return@repeat
                    if (attempt < 2) delay(800)
                }
            }

            if (routeSteps.isEmpty()) {
                routeSteps = buildList {
                    session.assignedDoctorName?.takeIf { it.isNotBlank() }?.let {
                        add("Консультация врача: $it")
                    } ?: add("Консультация врача")
                    labs.forEach { add("Анализ: $it") }
                }
            }

            val level = session.resolvedUrgencyLevel
            val urgencyTitle = when {
                level == null -> "Результат триажа"
                level >= 5 -> "Срочность: Экстренно"
                level >= 4 -> "Срочность: Срочно"
                level >= 3 -> "Срочность: В течение суток"
                level >= 2 -> "Срочность: Плановая"
                else -> "Срочность: Самонаблюдение"
            }
            val hyp = session.hypotheses.orEmpty()
                .mapNotNull { h ->
                    val name = h.condition?.trim().orEmpty()
                    if (name.isEmpty()) null
                    else if (h.probability != null) {
                        val pct = if (h.probability <= 1.0) (h.probability * 100).toInt() else h.probability.toInt()
                        "$name ($pct%)"
                    } else {
                        name
                    }
                }
                .joinToString(", ")

            _uiState.value = TriageResultUiState(
                isLoading = false,
                urgencyTitle = urgencyTitle + if (level != null) " · $level" else "",
                urgencySubtitle = hyp.ifBlank { session.recommendedSpecialization.orEmpty() },
                recommendation = session.resolvedRecommendation.orEmpty(),
                routeSteps = routeSteps,
                assignedDoctorId = session.assignedDoctorId,
                assignedDoctorName = session.assignedDoctorName,
                consultationSessionId = session.consultationSessionId,
                recommendedLabs = labs,
            )
        }
    }

    /** Like web: drop current triage and open a blank chat on the ИИ tab. */
    fun startNewTriage(onReady: () -> Unit) {
        viewModelScope.launch {
            sessionManager.requestNewTriage()
            onReady()
        }
    }
}
