package com.vitals.mobile.core.data.consultations

import kotlinx.serialization.Serializable

/** Canonical consultation types, matching VitalsWeb's `ConsultationType` union. */
enum class ConsultationType(val wireValue: String) {
    SYNC_CHAT("SyncChat"),
    VIDEO("Video"),
    ASYNC("Async"),
    IN_PERSON("InPerson"),
    HOME_VISIT("HomeVisit"),
}

@Serializable
data class CreateConsultationRequest(
    val doctorId: String,
    val patientId: String? = null,
    val consultationType: String? = null,
)

@Serializable
data class BookConsultationRequest(
    val doctorId: String,
    val slotId: String,
    val consultationType: String? = null,
    val urgencyLevel: String? = null,
    val triageSessionId: String? = null,
)

@Serializable
data class ConsentRequest(
    val dataProcessingConsent: Boolean,
    val videoRecordingConsent: Boolean,
)

@Serializable
data class SendConsultationMessageRequest(
    val messageType: String? = null,
    val content: String,
    val attachmentUrl: String? = null,
    val isImportant: Boolean? = null,
)

@Serializable
data class CancelConsultationRequest(val reason: String? = null)

@Serializable
data class JoinConsultationRequest(val role: String? = null)

@Serializable
data class RateConsultationRequest(
    val role: String? = null,
    val score: Int,
    val feedback: String? = null,
)

@Serializable
data class CompleteConsultationRequest(
    val complaints: String,
    val anamnesis: String,
    val examinationNotes: String? = null,
    val preliminaryDiagnosisIcd10: String,
    val preliminaryDiagnosisText: String,
    val recommendations: String,
    val prescriptions: List<String>? = null,
    val labOrders: List<String>? = null,
    val nextVisitDate: String? = null,
)

@Serializable
data class ConsultationProtocolDto(
    val complaints: String? = null,
    val anamnesis: String? = null,
    val examinationNotes: String? = null,
    val preliminaryDiagnosisIcd10: String? = null,
    val preliminaryDiagnosisText: String? = null,
    val recommendations: String? = null,
    val prescriptions: List<String>? = null,
    val labOrders: List<String>? = null,
    val nextVisitDate: String? = null,
)

@Serializable
data class ConsultationDto(
    val id: String? = null,
    val sessionId: String? = null,
    val doctorId: String? = null,
    val doctorName: String? = null,
    val patientId: String? = null,
    val patientName: String? = null,
    val consultationType: String? = null,
    val type: String? = null,
    val status: String? = null,
    val isScheduled: Boolean? = null,
    val scheduledAt: String? = null,
    val createdAt: String? = null,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val lastActivityAt: String? = null,
    val protocol: ConsultationProtocolDto? = null,
) {
    val resolvedId: String get() = id ?: sessionId.orEmpty()

    val resolvedType: String? get() = consultationType ?: type

    /** Как web: слот-запись, если API явно пометил или есть scheduledAt у незавершённой. */
    fun isSlotBooking(): Boolean {
        if (isScheduled == true) return true
        if (isScheduled == false) return false
        return !scheduledAt.isNullOrBlank()
    }
}
