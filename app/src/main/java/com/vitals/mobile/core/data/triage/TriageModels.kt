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
data class TriageHypothesisDto(
    val condition: String? = null,
    val probability: Double? = null,
)

@Serializable
data class TriageLlmResultDto(
    val urgencyLevel: Int? = null,
    val recommendedAction: String? = null,
    val readyToComplete: Boolean? = null,
    val completeSuggestion: String? = null,
    val hypotheses: List<TriageHypothesisDto>? = null,
    val emergencyWarning: Boolean? = null,
)

@Serializable
data class TriageAssessmentDto(
    val urgencyLevel: Int? = null,
    val assistantReply: String? = null,
    val readyToComplete: Boolean? = null,
    val completeSuggestion: String? = null,
    val llmResult: TriageLlmResultDto? = null,
)

@Serializable
data class TriageSessionDto(
    val id: String? = null,
    val sessionId: String? = null,
    val patientId: String? = null,
    val chiefComplaint: String? = null,
    val status: String? = null,
    val messages: List<ChatMessageDto>? = null,
    val urgency: String? = null,
    val urgencyLevel: Int? = null,
    val latestUrgencyLevel: Int? = null,
    val recommendation: String? = null,
    val recommendationText: String? = null,
    val recommendedSpecialization: String? = null,
    val canBeRemote: Boolean? = null,
    val hypotheses: List<TriageHypothesisDto>? = null,
    val recommendedLabs: List<String>? = null,
    val routingDecisionId: String? = null,
    val routingOutcomeType: String? = null,
    val assignedDoctorId: String? = null,
    val assignedDoctorName: String? = null,
    val consultationSessionId: String? = null,
    val readyToComplete: Boolean? = null,
    val completeSuggestion: String? = null,
    val latestAssessment: TriageAssessmentDto? = null,
) {
    val resolvedId: String get() = id ?: sessionId.orEmpty()

    val resolvedUrgencyLevel: Int?
        get() = urgencyLevel
            ?: latestUrgencyLevel
            ?: latestAssessment?.urgencyLevel
            ?: latestAssessment?.llmResult?.urgencyLevel

    val resolvedReadyToComplete: Boolean
        get() = readyToComplete == true ||
            latestAssessment?.readyToComplete == true ||
            latestAssessment?.llmResult?.readyToComplete == true

    val resolvedCompleteSuggestion: String?
        get() = completeSuggestion?.trim()?.takeIf { it.isNotEmpty() }
            ?: latestAssessment?.completeSuggestion?.trim()?.takeIf { it.isNotEmpty() }
            ?: latestAssessment?.llmResult?.completeSuggestion?.trim()?.takeIf { it.isNotEmpty() }

    val resolvedRecommendation: String?
        get() = recommendation?.trim()?.takeIf { it.isNotEmpty() }
            ?: recommendationText?.trim()?.takeIf { it.isNotEmpty() }
            ?: latestAssessment?.llmResult?.recommendedAction?.trim()?.takeIf { it.isNotEmpty() }
            ?: latestAssessment?.assistantReply?.trim()?.takeIf { it.isNotEmpty() }

    val isCompleted: Boolean
        get() = status.equals("completed", ignoreCase = true) ||
            !routingDecisionId.isNullOrBlank() ||
            !consultationSessionId.isNullOrBlank()
}
