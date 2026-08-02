package com.vitals.mobile.core.data.medicalrecords

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Event types written by the patient app, mirroring VitalsWeb's medical-record events. */
object MedicalRecordEventTypes {
    const val MOOD_CHECK = "mood_check"
    const val DOCUMENT = "document"
    const val SUPPORT_REQUEST = "support_request"
    const val HOUSE_CALL_REQUEST = "house_call_request"
}

const val SOURCE_SERVICE_PATIENT_PORTAL = "patient-portal"

@Serializable
data class AppendEventRequest(
    val eventType: String,
    val sourceService: String = SOURCE_SERVICE_PATIENT_PORTAL,
    val payloadJson: String,
    val occurredAt: String,
)

@Serializable
data class AccessGrantRequest(
    val granteeUserId: String,
    val granteeRole: String? = null,
    val expiresAt: String? = null,
    val reason: String? = null,
)

@Serializable
data class MedicalRecordEventDto(
    val id: String? = null,
    val eventId: String? = null,
    val eventType: String? = null,
    val sourceService: String? = null,
    val payloadJson: String? = null,
    /** Some gateways return structured payload instead of payloadJson. */
    val payload: JsonElement? = null,
    val occurredAt: String? = null,
    val createdAt: String? = null,
) {
    val resolvedId: String get() = id ?: eventId.orEmpty()
    val effectiveDate: String get() = occurredAt ?: createdAt.orEmpty()
}

// --- mood_check payload -----------------------------------------------------------------

enum class MoodCode(val wireValue: String, val label: String, val severity: Int) {
    BETTER("better", "Стало лучше", 1),
    SAME("same", "Без изменений", 2),
    WORSE("worse", "Стало хуже", 3),
}

@Serializable
data class MoodCheckPayload(
    val mood: String,
    val moodCode: String,
    val severity: Int,
    val notifyDoctor: Boolean = true,
    val escalate: Boolean = false,
)

@Serializable
data class DocumentEventPayload(
    val title: String,
    val docType: String,
    val hasPaper: Boolean = false,
    val hasDigital: Boolean = false,
)

@Serializable
data class SupportRequestPayload(
    val topic: String,
    val message: String,
)

@Serializable
data class HouseCallRequestPayload(
    val address: String,
    val symptoms: String,
    val desiredTime: String? = null,
    val phone: String? = null,
    val urgent: Boolean = false,
)

data class DiagnosisDto(
    val icd10Code: String? = null,
    val code: String? = null,
    val description: String? = null,
    val title: String? = null,
    val recordedAt: String? = null,
    val occurredAt: String? = null,
    val sourceEventId: String? = null,
) {
    val resolvedCode: String get() = icd10Code?.trim().orEmpty().ifBlank { code?.trim().orEmpty() }
    val resolvedTitle: String
        get() = description?.trim()?.takeIf { it.isNotEmpty() }
            ?: title?.trim()?.takeIf { it.isNotEmpty() }
            ?: resolvedCode.ifBlank { "Диагноз" }

    fun displayLabel(): String {
        val c = resolvedCode
        val t = resolvedTitle
        return when {
            c.isNotBlank() && t.isNotBlank() && !t.equals(c, true) -> "$c — $t"
            t.isNotBlank() -> t
            c.isNotBlank() -> c
            else -> "Диагноз"
        }
    }
}

data class PatientStateDto(
    val patientId: String? = null,
    val summary: String? = null,
    val allergies: String? = null,
    val bloodType: String? = null,
    val activeConditions: List<String> = emptyList(),
    val activeDiagnoses: List<DiagnosisDto> = emptyList(),
    val activeMedications: List<String> = emptyList(),
    val recentLabResults: List<String> = emptyList(),
    val lastVisitAt: String? = null,
)
