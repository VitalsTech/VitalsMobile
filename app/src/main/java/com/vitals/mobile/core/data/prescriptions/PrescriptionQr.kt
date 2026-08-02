package com.vitals.mobile.core.data.prescriptions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class PrescriptionQrResult(
    val bitmap: Bitmap? = null,
    val payloadPreview: String? = null,
    val errorMessage: String? = null,
)

/** Mirrors VitalsWeb `extractQrImageSrc` / `extractQrPayload` + backend `QrCodeResponse`. */
object PrescriptionQr {
    /** Backend returns `qrCodeBase64Png` (see PrescriptionService QrCodeResponse). */
    private val IMAGE_KEYS = listOf(
        "qrCodeBase64Png",
        "qrCodeBase64",
        "qrBase64",
        "imageBase64",
        "base64",
        "qrCode",
        "qrImage",
        "image",
        "qrUrl",
        "url",
        "dataUrl",
        "qr",
        "data",
    )
    private val PAYLOAD_KEYS = listOf(
        "payload", "content", "data", "code", "token", "value", "qrPayload", "text",
    )

    fun extractImageSrc(data: JsonElement?): String? {
        if (data == null) return null
        when (data) {
            is JsonPrimitive -> return imageSrcFromString(data.contentOrNull)
            is JsonObject -> {
                for (key in IMAGE_KEYS) {
                    val nested = extractImageSrc(objectValue(data, key))
                    if (nested != null) return nested
                }
                // Case-insensitive fallback for gateway casing quirks.
                for ((k, v) in data) {
                    if (IMAGE_KEYS.any { it.equals(k, ignoreCase = true) }) {
                        val nested = extractImageSrc(v)
                        if (nested != null) return nested
                    }
                }
            }
            else -> Unit
        }
        return null
    }

    fun extractPayload(data: JsonElement?): String? {
        if (data == null) return null
        when (data) {
            is JsonPrimitive -> {
                val trimmed = data.contentOrNull?.trim().orEmpty()
                if (trimmed.isEmpty()) return null
                if (trimmed.startsWith("data:image") || trimmed.startsWith("http")) return null
                if (looksLikeBase64Image(trimmed)) return null
                return trimmed
            }
            is JsonObject -> {
                for (key in PAYLOAD_KEYS) {
                    val value = (objectValue(data, key) as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
                    if (value.isEmpty()) continue
                    if (imageSrcFromString(value) != null) continue
                    return value
                }
            }
            else -> Unit
        }
        return null
    }

    fun fallbackQrUrl(payload: String): String {
        val encoded = URLEncoder.encode(payload, StandardCharsets.UTF_8.name())
        return "https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=$encoded"
    }

    fun decodeBitmap(src: String, http: OkHttpClient): Bitmap? {
        return when {
            src.startsWith("data:image") -> {
                val b64 = src.substringAfter("base64,", missingDelimiterValue = "")
                decodeBase64(b64)
            }
            src.startsWith("http://") || src.startsWith("https://") -> {
                val response = http.newCall(Request.Builder().url(src).build()).execute()
                response.use {
                    if (!it.isSuccessful) return null
                    val bytes = it.body?.bytes() ?: return null
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }
            looksLikeBase64Image(src) -> decodeBase64(src.replace("\\s".toRegex(), ""))
            else -> null
        }
    }

    private fun objectValue(obj: JsonObject, key: String): JsonElement? =
        obj[key] ?: obj.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value

    private fun imageSrcFromString(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("data:image") || trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        if (looksLikeBase64Image(trimmed)) {
            return "data:image/png;base64,${trimmed.replace("\\s".toRegex(), "")}"
        }
        return null
    }

    private fun looksLikeBase64Image(value: String): Boolean {
        val compact = value.replace("\\s".toRegex(), "")
        // PNG base64 from backend is typically hundreds of chars; keep threshold moderate.
        return compact.length > 64 && compact.matches(Regex("^[A-Za-z0-9+/=_-]+$"))
    }

    private fun decodeBase64(b64: String): Bitmap? {
        if (b64.isBlank()) return null
        return runCatching {
            val flags = Base64.DEFAULT or Base64.URL_SAFE
            val bytes = runCatching { Base64.decode(b64, Base64.DEFAULT) }.getOrNull()
                ?: Base64.decode(b64, flags)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }
}
