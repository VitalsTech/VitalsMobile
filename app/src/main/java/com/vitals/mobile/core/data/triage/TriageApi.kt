package com.vitals.mobile.core.data.triage

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TriageApi {
    @POST("api/v1/triage/sessions")
    suspend fun createSession(@Body request: CreateTriageSessionRequest): TriageSessionDto

    @GET("api/v1/triage/sessions/{sessionId}")
    suspend fun getSession(@Path("sessionId") sessionId: String): TriageSessionDto

    @POST("api/v1/triage/sessions/{sessionId}/messages")
    suspend fun sendMessage(@Path("sessionId") sessionId: String, @Body request: TriageMessageRequest): JsonElement

    @POST("api/v1/triage/sessions/{sessionId}/complete")
    suspend fun completeSession(@Path("sessionId") sessionId: String): TriageSessionDto

    @GET("api/v1/triage/patients/{patientId}/sessions")
    suspend fun getPatientSessions(
        @Path("patientId") patientId: String,
        @Query("limit") limit: Int? = null,
    ): JsonElement
}
