package com.vitals.mobile.core.network

import kotlinx.serialization.json.JsonObject

data class ParsedTokens(
    val accessToken: String?,
    val refreshToken: String?,
)

/**
 * Extracts session tokens from a loosely-typed auth response body, mirroring VitalsWeb's approach:
 * look for `accessToken` | `access_token` | `token`, and `refreshToken` | `refresh_token`, optionally
 * nested one level under common wrapper keys (`data`, `result`, `session`).
 */
object SessionTokenParser {
    private val wrapperKeys = listOf("data", "result", "session", "payload")

    fun extract(root: JsonObject): ParsedTokens? {
        val direct = extractDirect(root)
        if (direct.accessToken != null) return direct

        for (key in wrapperKeys) {
            val nested = root.objectField(key) ?: continue
            val parsed = extractDirect(nested)
            if (parsed.accessToken != null) return parsed
        }
        return if (direct.accessToken != null || direct.refreshToken != null) direct else null
    }

    private fun extractDirect(obj: JsonObject): ParsedTokens = ParsedTokens(
        accessToken = obj.stringField("accessToken", "access_token", "token"),
        refreshToken = obj.stringField("refreshToken", "refresh_token"),
    )
}
