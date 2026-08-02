package com.vitals.mobile.core.data.auth

import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthApi {

    @Headers("X-Vitals-Skip-Auth: 1")
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): JsonObject

    @Headers("X-Vitals-Skip-Auth: 1")
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): JsonObject

    @Headers("X-Vitals-Skip-Auth: 1")
    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): JsonObject

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body request: LogoutRequest)

    @POST("api/v1/auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest)

    @Headers("X-Vitals-Skip-Auth: 1")
    @POST("api/v1/auth/password/forgot")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest)

    @Headers("X-Vitals-Skip-Auth: 1")
    @POST("api/v1/auth/password/reset")
    suspend fun resetPassword(@Body request: ResetPasswordRequest)
}
