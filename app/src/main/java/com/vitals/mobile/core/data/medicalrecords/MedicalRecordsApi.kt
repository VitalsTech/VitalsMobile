package com.vitals.mobile.core.data.medicalrecords

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MedicalRecordsApi {
    @POST("api/v1/medical-records/patients/{patientId}/events")
    suspend fun appendEvent(@Path("patientId") patientId: String, @Body request: AppendEventRequest): MedicalRecordEventDto

    @GET("api/v1/medical-records/patients/{patientId}/history")
    suspend fun getHistory(
        @Path("patientId") patientId: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("eventTypes") eventTypes: String? = null,
    ): JsonElement

    @GET("api/v1/medical-records/patients/{patientId}/state")
    suspend fun getState(@Path("patientId") patientId: String): JsonElement

    @GET("api/v1/medical-records/patients/{patientId}/attachments")
    suspend fun getAttachments(@Path("patientId") patientId: String): JsonElement

    @POST("api/v1/medical-records/patients/{patientId}/access-grants")
    suspend fun createAccessGrant(@Path("patientId") patientId: String, @Body request: AccessGrantRequest): JsonElement

    @DELETE("api/v1/medical-records/patients/{patientId}/access-grants/{grantId}")
    suspend fun revokeAccessGrant(@Path("patientId") patientId: String, @Path("grantId") grantId: String)
}
