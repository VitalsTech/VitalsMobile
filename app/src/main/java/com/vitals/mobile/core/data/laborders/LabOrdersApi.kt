package com.vitals.mobile.core.data.laborders

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface LabOrdersApi {
    @POST("api/v1/lab-orders")
    suspend fun create(@Body request: CreateLabOrderRequest): LabOrderDto

    @GET("api/v1/lab-orders/{id}")
    suspend fun get(@Path("id") id: String): LabOrderDto

    @GET("api/v1/lab-orders/patients/{patientId}")
    suspend fun getForPatient(@Path("patientId") patientId: String): JsonElement

    @POST("api/v1/lab-orders/{id}/start")
    suspend fun start(@Path("id") id: String, @Body request: StartLabOrderRequest)

    @POST("api/v1/lab-orders/{id}/cancel")
    suspend fun cancel(@Path("id") id: String, @Body request: CancelLabOrderRequest)

    @POST("api/v1/lab-orders/{id}/results")
    suspend fun submitResults(@Path("id") id: String, @Body request: LabResultsRequest): LabOrderDto
}
