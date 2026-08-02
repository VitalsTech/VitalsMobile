package com.vitals.mobile.core.data.triage

import com.vitals.mobile.core.data.common.ChatMessageDto
import kotlinx.serialization.Serializable

@Serializable
data class CreateTriageSessionRequest(
    val patientId: String,
    val chiefComplaint: String? = null,
    val locale: String = "ru",
)

@Serializable
data class TriageMessageRequest(val message: String)

@Serializable
data class TriageSessionDto(
    val id: String? = null,
    val sessionId: String? = null,
    val patientId: String? = null,
    val chiefComplaint: String? = null,
    val status: String? = null,
    val messages: List<ChatMessageDto>? = null,
    val urgencyLevel: String? = null,
    val hypotheses: List<String>? = null,
    val recommendedLabs: List<String>? = null,
    val routingDecisionId: String? = null,
    val routingOutcomeType: String? = null,
    val assignedDoctorId: String? = null,
    val assignedDoctorName: String? = null,
    val consultationSessionId: String? = null,
) {
    val resolvedId: String get() = id ?: sessionId.orEmpty()
}
