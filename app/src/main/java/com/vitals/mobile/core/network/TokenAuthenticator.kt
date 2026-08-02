package com.vitals.mobile.core.network

import com.vitals.mobile.core.session.SessionManager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

/**
 * Performs a single automatic refresh-and-retry on 401, mirroring VitalsWeb's `http.ts` behaviour:
 * `POST /api/v1/auth/refresh` with `{ refreshToken, deviceFingerprint }`, then retries the original
 * request once with the new access token. On failure the local session is cleared.
 */
class TokenAuthenticator(
    private val plainClient: OkHttpClient,
    private val sessionManager: SessionManager,
) : Authenticator {

    private val json = Json { ignoreUnknownKeys = true }

    override fun authenticate(route: Route?, response: Response): Request? = runBlocking {
        if (responseCount(response) >= 2) return@runBlocking null

        val session = sessionManager.currentSession()
        val refreshToken = session.refreshToken ?: return@runBlocking null

        val body = buildJsonObject {
            put("refreshToken", refreshToken)
            put("deviceFingerprint", session.deviceFingerprint)
        }.toString().toRequestBody("application/json".toMediaType())

        val refreshRequest = Request.Builder()
            .url(ApiConfig.BASE_URL.trimEnd('/') + "/api/v1/auth/refresh")
            .header(HEADER_SKIP_AUTH, "1")
            .post(body)
            .build()

        val newAccessToken = runCatching {
            plainClient.newCall(refreshRequest).execute().use { refreshResponse ->
                if (!refreshResponse.isSuccessful) return@use null
                val text = refreshResponse.body?.string() ?: return@use null
                val jsonObj = json.parseToJsonElement(text) as? JsonObject ?: return@use null
                val tokens = SessionTokenParser.extract(jsonObj)
                if (tokens?.accessToken != null) {
                    sessionManager.saveTokens(tokens.accessToken, tokens.refreshToken ?: refreshToken)
                    tokens.accessToken
                } else {
                    null
                }
            }
        }.getOrNull()

        if (newAccessToken == null) {
            sessionManager.clear()
            return@runBlocking null
        }

        response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
