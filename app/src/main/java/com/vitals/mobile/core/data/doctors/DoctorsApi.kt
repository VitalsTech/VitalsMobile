package com.vitals.mobile.core.data.doctors

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DoctorsApi {
    @GET("api/v1/doctors")
    suspend fun listDoctors(
        @Query("specialization") specialization: String? = null,
        @Query("query") query: String? = null,
        @Query("page") page: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
    ): JsonElement

    @GET("api/v1/doctors/{doctorId}")
    suspend fun getDoctor(@Path("doctorId") doctorId: String): DoctorDto

    @PATCH("api/v1/doctors/me/profile")
    suspend fun updateMyProfile(@Body request: DoctorProfileUpdateRequest): DoctorDto

    @GET("api/v1/doctors/{doctorId}/schedule")
    suspend fun getSchedule(
        @Path("doctorId") doctorId: String,
        @Query("from") from: String? = null,
        @Query("days") days: Int? = null,
    ): JsonElement

    @GET("api/v1/doctors/me/calendar")
    suspend fun getMyCalendar(
        @Query("from") from: String? = null,
        @Query("days") days: Int? = null,
    ): JsonElement

    @POST("api/v1/doctors/me/schedule/slots")
    suspend fun createScheduleSlot(@Body request: DoctorScheduleSlotPayload): ScheduleSlotDto

    @DELETE("api/v1/doctors/me/schedule/slots/{slotId}")
    suspend fun deleteScheduleSlot(@Path("slotId") slotId: String)
}
