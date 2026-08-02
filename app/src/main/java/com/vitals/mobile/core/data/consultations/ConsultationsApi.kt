package com.vitals.mobile.core.data.consultations

import com.vitals.mobile.core.data.common.ChatMessageDto
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ConsultationsApi {
    @POST("api/v1/consultations")
    suspend fun create(@Body request: CreateConsultationRequest): ConsultationDto

    @POST("api/v1/consultations/book")
    suspend fun book(@Body request: BookConsultationRequest): ConsultationDto

    @GET("api/v1/consultations/{sessionId}")
    suspend fun get(@Path("sessionId") sessionId: String): ConsultationDto

    @GET("api/v1/consultations/mine")
    suspend fun mine(
        @Query("includeCompleted") includeCompleted: Boolean? = null,
        @Query("limit") limit: Int? = null,
    ): JsonElement

    @GET("api/v1/consultations/active")
    suspend fun active(
        @Query("patientId") patientId: String? = null,
        @Query("doctorId") doctorId: String? = null,
    ): JsonElement

    @POST("api/v1/consultations/{id}/join")
    suspend fun join(@Path("id") id: String, @Body request: JoinConsultationRequest)

    @POST("api/v1/consultations/{id}/consent")
    suspend fun consent(@Path("id") id: String, @Body request: ConsentRequest)

    @GET("api/v1/consultations/{id}/messages")
    suspend fun getMessages(
        @Path("id") id: String,
        @Query("afterSequence") afterSequence: Long? = null,
        @Query("markAsRead") markAsRead: Boolean? = null,
    ): List<ChatMessageDto>

    @POST("api/v1/consultations/{id}/messages/read")
    suspend fun markMessagesRead(@Path("id") id: String)

    @POST("api/v1/consultations/{id}/messages")
    suspend fun sendMessage(@Path("id") id: String, @Body request: SendConsultationMessageRequest): ChatMessageDto

    @POST("api/v1/consultations/{id}/pause")
    suspend fun pause(@Path("id") id: String)

    @POST("api/v1/consultations/{id}/resume")
    suspend fun resume(@Path("id") id: String)

    @POST("api/v1/consultations/{id}/confirm")
    suspend fun confirm(@Path("id") id: String)

    @POST("api/v1/consultations/{id}/cancel")
    suspend fun cancel(@Path("id") id: String, @Body request: CancelConsultationRequest)

    @POST("api/v1/consultations/{id}/complete")
    suspend fun complete(@Path("id") id: String, @Body request: CompleteConsultationRequest): ConsultationDto

    @POST("api/v1/consultations/{id}/ratings")
    suspend fun rate(@Path("id") id: String, @Body request: RateConsultationRequest)
}
