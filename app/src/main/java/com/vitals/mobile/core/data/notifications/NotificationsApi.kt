package com.vitals.mobile.core.data.notifications

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface NotificationsApi {
    @POST("api/v1/notifications/push-tokens")
    suspend fun registerPushToken(@Body request: RegisterPushTokenRequest)

    @GET("api/v1/notifications/preferences")
    suspend fun getPreferences(): UserPreferenceDto

    @PUT("api/v1/notifications/preferences")
    suspend fun updatePreferences(@Body request: UserPreferenceDto): UserPreferenceDto

    @GET("api/v1/notifications/history")
    suspend fun getHistory(@Query("limit") limit: Int? = null): JsonElement
}
