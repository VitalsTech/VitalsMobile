package com.vitals.mobile.core.data.prescriptions

import com.vitals.mobile.core.network.asArrayFlexible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PrescriptionsRepository @Inject constructor(
    private val api: PrescriptionsApi,
    private val json: Json,
    @Named("plain") private val plainHttp: OkHttpClient,
) {
    suspend fun getForPatient(patientId: String): List<PrescriptionDto> {
        val raw = api.getForPatient(patientId)
        return raw.asArrayFlexible("items", "data", "prescriptions").mapNotNull { element ->
            runCatching { json.decodeFromJsonElement<PrescriptionDto>(element) }.getOrNull()
        }
    }

    suspend fun getForPatientAliases(patientIds: Collection<String>): List<PrescriptionDto> {
        val unique = patientIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val byId = linkedMapOf<String, PrescriptionDto>()
        for (id in unique) {
            val chunk = runCatching { getForPatient(id) }.getOrElse { emptyList() }
            for (rx in chunk) {
                val key = rx.resolvedId.ifBlank { "anon-${byId.size}" }
                byId.putIfAbsent(key, rx)
            }
        }
        return byId.values.toList()
    }

    suspend fun get(id: String): PrescriptionDto = api.get(id)

    suspend fun getInstructionsText(id: String): String? {
        val raw = api.getInstructions(id)
        return instructionsToText(raw)
    }

    suspend fun loadQr(id: String): PrescriptionQrResult = withContext(Dispatchers.IO) {
        try {
            val data = api.getQr(id)
            // Backend: { prescriptionId, payload, qrCodeBase64Png }
            val src = PrescriptionQr.extractImageSrc(data)
            val payload = PrescriptionQr.extractPayload(data)
            val bitmapFromApi = src?.let { PrescriptionQr.decodeBitmap(it, plainHttp) }
            if (bitmapFromApi != null) {
                return@withContext PrescriptionQrResult(
                    bitmap = bitmapFromApi,
                    payloadPreview = payload?.let { if (it.length > 120) it.take(120) + "…" else it },
                )
            }
            if (!payload.isNullOrBlank()) {
                val fallback = PrescriptionQr.fallbackQrUrl(payload)
                val bitmap = PrescriptionQr.decodeBitmap(fallback, plainHttp)
                if (bitmap != null) {
                    return@withContext PrescriptionQrResult(
                        bitmap = bitmap,
                        payloadPreview = if (payload.length > 120) payload.take(120) + "…" else payload,
                    )
                }
            }
            PrescriptionQrResult(
                errorMessage = "QR пока недоступен для этого рецепта.",
            )
        } catch (error: Exception) {
            PrescriptionQrResult(errorMessage = error.message ?: "Не удалось загрузить QR.")
        }
    }

    suspend fun sendToPharmacy(id: String): PrescriptionDto =
        api.sendToPharmacy(id, SendToPharmacyRequest(autoSelectNearest = true))

    suspend fun cancel(id: String, reason: String) {
        api.cancel(id, CancelPrescriptionRequest(reason = reason))
    }

    private fun instructionsToText(data: JsonElement?): String? {
        if (data == null) return null
        when (data) {
            is JsonPrimitive -> {
                val trimmed = data.contentOrNull?.trim().orEmpty()
                if (trimmed.isEmpty()) return null
                if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                    return runCatching { instructionsToText(json.parseToJsonElement(trimmed)) }.getOrNull()
                        ?: trimmed
                }
                return trimmed
            }
            is JsonObject -> {
                for (key in listOf(
                    "instructions", "instruction", "text", "content",
                    "message", "patientInstructions", "summary",
                )) {
                    val value = (data[key] as? JsonPrimitive)?.contentOrNull?.trim()
                    if (!value.isNullOrEmpty()) return value
                }
                return null
            }
            else -> return null
        }
    }
}
