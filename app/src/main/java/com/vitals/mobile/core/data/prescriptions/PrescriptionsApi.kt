package com.vitals.mobile.core.data.prescriptions

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PrescriptionsApi {
    @POST("api/v1/prescriptions")
    suspend fun create(@Body request: CreatePrescriptionRequest): PrescriptionDto

    @GET("api/v1/prescriptions/{id}")
    suspend fun get(@Path("id") id: String): PrescriptionDto

    @GET("api/v1/prescriptions/patients/{patientId}")
    suspend fun getForPatient(@Path("patientId") patientId: String): JsonElement

    @POST("api/v1/prescriptions/{id}/sign")
    suspend fun sign(@Path("id") id: String, @Query("confirmWarnings") confirmWarnings: Boolean = true): PrescriptionDto

    @POST("api/v1/prescriptions/{id}/send-to-pharmacy")
    suspend fun sendToPharmacy(@Path("id") id: String, @Body request: SendToPharmacyRequest): PrescriptionDto

    @POST("api/v1/prescriptions/{id}/cancel")
    suspend fun cancel(@Path("id") id: String, @Body request: CancelPrescriptionRequest)

    @POST("api/v1/prescriptions/{id}/validate")
    suspend fun validate(@Path("id") id: String): JsonElement

    @GET("api/v1/prescriptions/{id}/instructions")
    suspend fun getInstructions(@Path("id") id: String): JsonElement

    @GET("api/v1/prescriptions/{id}/qr")
    suspend fun getQr(@Path("id") id: String): JsonElement
}
