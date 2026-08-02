package com.vitals.mobile.core.data.routing

import kotlinx.serialization.Serializable

@Serializable
data class RoutingStepDto(
    val action: String? = null,
    val label: String? = null,
    val status: String? = null,
)

@Serializable
data class RoutingDecisionDto(
    val id: String? = null,
    val patientId: String? = null,
    val outcomeType: String? = null,
    val steps: List<RoutingStepDto>? = null,
)

@Serializable
data class ActiveRouteDto(
    val patientId: String? = null,
    val currentStepIndex: Int? = null,
    val steps: List<RoutingStepDto>? = null,
)
