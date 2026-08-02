package com.vitals.mobile.core.data.routing

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutingRepository @Inject constructor(
    private val api: RoutingApi,
) {
    suspend fun getActiveRoute(patientId: String): ActiveRouteDto = api.getActiveRoute(patientId)

    suspend fun getDecision(decisionId: String): RoutingDecisionDto = api.getDecision(decisionId)
}
