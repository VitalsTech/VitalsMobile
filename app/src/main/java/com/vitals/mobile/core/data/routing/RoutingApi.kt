package com.vitals.mobile.core.data.routing

import retrofit2.http.GET
import retrofit2.http.Path

interface RoutingApi {
    @GET("api/v1/routing/decisions/{decisionId}")
    suspend fun getDecision(@Path("decisionId") decisionId: String): RoutingDecisionDto

    @GET("api/v1/routing/patients/{patientId}/active-route")
    suspend fun getActiveRoute(@Path("patientId") patientId: String): ActiveRouteDto
}
