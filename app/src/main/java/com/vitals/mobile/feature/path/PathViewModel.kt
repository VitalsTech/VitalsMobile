package com.vitals.mobile.feature.path

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.common.ConsultationLabels
import com.vitals.mobile.core.data.common.RouteLabels
import com.vitals.mobile.core.data.consultations.ConsultationsRepository
import com.vitals.mobile.core.data.laborders.LabOrdersRepository
import com.vitals.mobile.core.data.medicalrecords.MedicalRecordsRepository
import com.vitals.mobile.core.data.medicalrecords.MoodCode
import com.vitals.mobile.core.data.prescriptions.PrescriptionsRepository
import com.vitals.mobile.core.data.routing.RoutingRepository
import com.vitals.mobile.core.data.triage.TriageRepository
import com.vitals.mobile.core.data.users.UsersRepository
import com.vitals.mobile.core.navigation.NavRoutes
import com.vitals.mobile.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PathContinueTarget {
    TRIAGE,
    LABS,
    DOCTORS,
    CONSULTATIONS,
    OVERVIEW,
}

data class RouteStepUi(
    val index: Int,
    val title: String,
    val status: String,
    val description: String = "",
    val isCurrent: Boolean,
    val action: String? = null,
)

data class PathUiState(
    val isLoading: Boolean = true,
    val greetingName: String = "",
    val currentStepIndex: Int = 0,
    val totalSteps: Int = 4,
    val steps: List<RouteStepUi> = emptyList(),
    val continueTarget: PathContinueTarget = PathContinueTarget.TRIAGE,
    val continueLabel: String = "Продолжить маршрут",
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

@HiltViewModel
class PathViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val usersRepository: UsersRepository,
    private val routingRepository: RoutingRepository,
    private val medicalRecordsRepository: MedicalRecordsRepository,
    private val consultationsRepository: ConsultationsRepository,
    private val labOrdersRepository: LabOrdersRepository,
    private val prescriptionsRepository: PrescriptionsRepository,
    private val triageRepository: TriageRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PathUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun reload() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val session = sessionManager.currentSession()
            val name = session.publicId?.let { publicId ->
                runCatching { usersRepository.getUser(publicId) }.getOrNull()?.let { user ->
                    user.firstName ?: user.fullName
                }
            }.orEmpty()

            val patientId = session.patientId
            val aliases = listOfNotNull(patientId, session.publicId).distinct()
            if (patientId == null) {
                _uiState.value = PathUiState(
                    isLoading = false,
                    greetingName = name.ifBlank { "пациент" },
                    errorMessage = "Профиль пациента не найден",
                    steps = emptyFourSteps(),
                )
                return@launch
            }

            val route = runCatching { routingRepository.getActiveRoute(patientId) }.getOrNull()
            val decisionId = route?.resolvedDecisionId
            val decision = decisionId?.let { runCatching { routingRepository.getDecision(it) }.getOrNull() }
            val specialty = decision?.specialist ?: decision?.recommendedSpecialization

            val triageSession = session.triageSessionId?.let {
                runCatching { triageRepository.getSession(it) }.getOrNull()
            }
            val triageDone = triageSession?.isCompleted == true ||
                !route?.steps.isNullOrEmpty() ||
                !decisionId.isNullOrBlank()

            val mine = runCatching { consultationsRepository.mine(includeCompleted = true, limit = 50) }
                .getOrElse { emptyList() }
            // Как web Home: completed ИЛИ есть протокол.
            val consultDone = mine.any {
                it.status.equals("Completed", ignoreCase = true) || it.protocol != null
            } || route?.steps.orEmpty().any {
                RouteLabels.isConsultationAction(it) && RouteLabels.isStepDone(it.status)
            }
            val hasOpenConsult = mine.any { !ConsultationLabels.isTerminal(it.status) }

            val labs = runCatching { labOrdersRepository.getForPatientAliases(aliases) }.getOrElse { emptyList() }
            val protocolLabs = mine.flatMap { c ->
                c.protocol?.labOrders.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }
            }.distinct()
            val recommended = buildList {
                addAll(route?.recommendedLabs.orEmpty())
                addAll(decision?.recommendedLabs.orEmpty())
                addAll(triageSession?.recommendedLabs.orEmpty())
            }.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

            val hasAnyLabOrders = labs.isNotEmpty()
            val hasOpenLabOrders = labs.any {
                val s = it.status?.lowercase().orEmpty()
                s == "ordered" || s == "inprogress" || s == "in_progress"
            }
            // Как web labsPending
            val labsPending = hasOpenLabOrders || hasAnyLabOrders ||
                protocolLabs.isNotEmpty() || recommended.isNotEmpty() ||
                route?.steps.orEmpty().any { RouteLabels.isLabAction(it) }

            val prescriptions = runCatching { prescriptionsRepository.getForPatientAliases(aliases) }
                .getOrElse { emptyList() }
            val protocolHasPrescription = mine.any { c ->
                c.protocol?.prescriptions.orEmpty().any { it.trim().isNotEmpty() }
            }
            // Как web hasPrescription — наличие факт наличия рецепта, не только «выдан».
            val hasPrescription = prescriptions.isNotEmpty() || protocolHasPrescription

            val steps = buildFourSteps(
                triageDone = triageDone,
                consultDone = consultDone,
                hasOpenConsult = hasOpenConsult,
                labsPending = labsPending,
                hasAnyLabOrders = hasAnyLabOrders,
                hasOpenLabOrders = hasOpenLabOrders,
                hasPrescription = hasPrescription,
                specialty = specialty,
                recommendedLabs = recommended,
                protocolLabs = protocolLabs,
            )
            val currentIndex = steps.indexOfFirst { it.isCurrent }.coerceAtLeast(0)
            val (target, label) = continueFor(steps.getOrNull(currentIndex), triageDone, hasOpenConsult)

            _uiState.value = PathUiState(
                isLoading = false,
                greetingName = name.ifBlank { "пациент" },
                currentStepIndex = currentIndex + 1,
                totalSteps = steps.size.coerceAtLeast(4),
                steps = steps,
                continueTarget = target,
                continueLabel = label,
            )
        }
    }

    private fun emptyFourSteps() = buildFourSteps(
        triageDone = false,
        consultDone = false,
        hasOpenConsult = false,
        labsPending = false,
        hasAnyLabOrders = false,
        hasOpenLabOrders = false,
        hasPrescription = false,
        specialty = null,
        recommendedLabs = emptyList(),
        protocolLabs = emptyList(),
    )

    /**
     * Статусы шагов как на web Home.pathSteps, затем один current = первый незавершённый.
     * Если все done — текущий = последний (шаг 4), чтобы не уезжать на «шаг 1».
     */
    private fun buildFourSteps(
        triageDone: Boolean,
        consultDone: Boolean,
        hasOpenConsult: Boolean,
        labsPending: Boolean,
        hasAnyLabOrders: Boolean,
        hasOpenLabOrders: Boolean,
        hasPrescription: Boolean,
        specialty: String?,
        recommendedLabs: List<String>,
        protocolLabs: List<String>,
    ): List<RouteStepUi> {
        data class Draft(val title: String, val description: String, var status: String, val action: String)

        val triageDesc = when {
            !triageDone -> "Опишите симптомы — система подберёт маршрут"
            !specialty.isNullOrBlank() -> "Завершён · рекомендован специалист: $specialty"
            else -> "Завершён"
        }

        val labDescription = when {
            hasOpenLabOrders || hasAnyLabOrders -> "Направления на вкладке «Анализы и рецепты»"
            protocolLabs.isNotEmpty() ->
                "Из протокола: ${protocolLabs.take(3).joinToString(", ")}${if (protocolLabs.size > 3) "…" else ""}"
            recommendedLabs.isNotEmpty() ->
                "Рекомендовано: ${recommendedLabs.take(3).joinToString(", ")}${if (recommendedLabs.size > 3) "…" else ""}"
            else -> "Ожидают назначения после приёма"
        }

        // Статусы до нормализации — логика web.
        val labsStatus = when {
            !consultDone -> "upcoming"
            labsPending && !hasPrescription -> "current"
            labsPending && hasPrescription -> "done"
            else -> "upcoming"
        }
        val rxStatus = when {
            !consultDone -> "upcoming"
            hasPrescription -> "done"
            else -> "current"
        }

        val drafts = mutableListOf(
            Draft(
                title = "ИИ-триаж",
                description = triageDesc,
                status = if (triageDone) "done" else "current",
                action = "triage",
            ),
            Draft(
                title = if (!specialty.isNullOrBlank()) "Консультация: $specialty" else "Консультация врача",
                description = when {
                    consultDone -> "Консультация завершена"
                    hasOpenConsult -> "Консультация в процессе"
                    else -> "После триажа запишемся к нужному специалисту"
                },
                status = if (consultDone) "done" else if (triageDone) "current" else "upcoming",
                action = "consultation",
            ),
            Draft(
                title = "Анализы",
                description = labDescription,
                status = labsStatus,
                action = "lab.order",
            ),
            Draft(
                title = "Получение рецепта",
                description = if (hasPrescription) {
                    "Рецепт оформлен · аптека-партнёр"
                } else {
                    "Аптека-партнёр · после назначения врача"
                },
                status = rxStatus,
                action = "prescription",
            ),
        )

        // Один текущий — первый не-done; если все done — последний шаг остаётся current.
        var sawCurrent = false
        drafts.forEach { draft ->
            if (draft.status == "done") return@forEach
            if (!sawCurrent) {
                draft.status = "current"
                sawCurrent = true
            } else {
                draft.status = "upcoming"
            }
        }
        if (!sawCurrent && drafts.isNotEmpty()) {
            // Все выполнены — показываем шаг 4 из 4 (в отличие от web fallback на 1).
            drafts.last().status = "current"
        }

        return drafts.mapIndexed { index, draft ->
            RouteStepUi(
                index = index + 1,
                title = draft.title,
                description = draft.description,
                status = RouteLabels.stepStatus(draft.status),
                isCurrent = draft.status == "current",
                action = draft.action,
            )
        }
    }

    private fun continueFor(
        current: RouteStepUi?,
        triageDone: Boolean,
        hasOpenConsult: Boolean,
    ): Pair<PathContinueTarget, String> {
        if (!triageDone) return PathContinueTarget.TRIAGE to "Начать ИИ-триаж"
        return when (current?.action) {
            "triage" -> PathContinueTarget.TRIAGE to "Продолжить триаж"
            "consultation" -> if (hasOpenConsult) {
                PathContinueTarget.CONSULTATIONS to "Открыть чат с врачом"
            } else {
                PathContinueTarget.DOCTORS to "Записаться к врачу"
            }
            "lab.order", "prescription" -> PathContinueTarget.LABS to "Анализы и рецепты"
            else -> PathContinueTarget.OVERVIEW to "Сводка и диагнозы"
        }
    }

    fun continueRoute(): String = when (_uiState.value.continueTarget) {
        PathContinueTarget.TRIAGE -> NavRoutes.TRIAGE_CHAT
        PathContinueTarget.LABS -> NavRoutes.LABS
        PathContinueTarget.DOCTORS -> NavRoutes.DOCTORS
        PathContinueTarget.CONSULTATIONS -> NavRoutes.CONSULTATIONS
        PathContinueTarget.OVERVIEW -> NavRoutes.MEDICAL_OVERVIEW
    }

    fun reportFeelingWorse() {
        viewModelScope.launch {
            val id = sessionManager.currentSession().patientId ?: return@launch
            runCatching { medicalRecordsRepository.recordMoodCheck(id, MoodCode.WORSE) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(infoMessage = "Врач уведомлён о ухудшении состояния")
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(infoMessage = "Не удалось отправить уведомление")
                }
        }
    }

    fun consumeInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }
}
