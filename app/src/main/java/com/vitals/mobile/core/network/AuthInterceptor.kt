package com.vitals.mobile.core.network

import com.vitals.mobile.core.session.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/** Marks a Retrofit request as not requiring an `Authorization` header (login/register/refresh/etc). */
const val HEADER_SKIP_AUTH = "X-Vitals-Skip-Auth"

/**
 * Attaches `Authorization: Bearer <accessToken>` to every request unless it carries [HEADER_SKIP_AUTH].
 * Mirrors the behaviour of VitalsWeb's `rawRequest` in `src/api/http.ts`.
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.header(HEADER_SKIP_AUTH) != null) {
            val cleaned = original.newBuilder().removeHeader(HEADER_SKIP_AUTH).build()
            return chain.proceed(cleaned)
        }

        val token = runBlocking { sessionManager.getAccessTokenBlocking() }
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            original
        }
        return chain.proceed(request)
    }
}
