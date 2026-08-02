package com.vitals.mobile.core.network

import android.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Decodes the payload segment of a JWT without verifying its signature (client-side display only). */
object JwtUtils {
    private val json = Json { ignoreUnknownKeys = true }

    fun decodePayload(token: String): JsonObject? {
        val parts = token.split(".")
        if (parts.size < 2) return null
        return runCatching {
            val decoded = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            json.parseToJsonElement(String(decoded, Charsets.UTF_8)).let { it as JsonObject }
        }.getOrNull()
    }

    /** Vitals JWTs carry the user's publicId under `sub` and the active profile id under `profile_id`. */
    fun publicId(token: String): String? = decodePayload(token)?.stringField("sub", "publicId", "public_id")

    fun profileId(token: String): String? = decodePayload(token)?.stringField("profile_id", "profileId")
}
