package com.vitals.mobile.core.data.routing

import kotlinx.serialization.Serializable

@Serializable
data class RoutingStepDto(
    val action: String? = null,
    val type: String? = null,
    val label: String? = null,
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val stepNumber: Int? = null,
    val order: Int? = null,
)

@Serializable
data class RoutingDecisionDto(
    val id: String? = null,
    val decisionId: String? = null,
    val patientId: String? = null,
    val outcomeType: String? = null,
    val specialist: String? = null,
    val recommendedSpecialization: String? = null,
    val recommendedLabs: List<String>? = null,
    val steps: List<RoutingStepDto>? = null,
) {
    val resolvedId: String get() = decisionId ?: id.orEmpty()
}

@Serializable
data class ActiveRouteDto(
    val patientId: String? = null,
    /** 1-based current step (gateway / web). */
    val currentStep: Int? = null,
    /** Legacy 0-based index. */
    val currentStepIndex: Int? = null,
    val currentDecisionId: String? = null,
    val decisionId: String? = null,
    val recommendedLabs: List<String>? = null,
    val steps: List<RoutingStepDto>? = null,
) {
    val resolvedDecisionId: String?
        get() = currentDecisionId ?: decisionId

    /**
     * 0-based index of the current step for UI.
     * Prefer `currentStep` (1-based) like VitalsWeb Home.
     */
    fun resolvedCurrentIndex(stepCount: Int): Int {
        if (stepCount <= 0) return 0
        currentStep?.let { oneBased ->
            if (oneBased > 0) return (oneBased - 1).coerceIn(0, stepCount - 1)
        }
        currentStepIndex?.let { return it.coerceIn(0, stepCount - 1) }
        return 0
    }
}
