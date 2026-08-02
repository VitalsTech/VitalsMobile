package com.vitals.mobile.core.data.users

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface UsersApi {
    @GET("api/v1/users/{publicId}")
    suspend fun getUser(@Path("publicId") publicId: String): UserDto

    @GET("api/v1/users/{publicId}/profiles")
    suspend fun getProfiles(@Path("publicId") publicId: String): JsonElement

    @POST("api/v1/users/add-profile")
    suspend fun addProfile(@Body request: AddProfileRequest)

    @POST("api/v1/users/switch-profile")
    suspend fun switchProfile(@Body request: SwitchProfileRequest)

    @GET("api/v1/users/{publicId}/has-profile/{profileType}")
    suspend fun hasProfile(@Path("publicId") publicId: String, @Path("profileType") profileType: String): JsonElement
}
