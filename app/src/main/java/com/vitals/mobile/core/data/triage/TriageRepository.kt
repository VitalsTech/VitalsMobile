package com.vitals.mobile.core.data.triage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TriageRepository @Inject constructor(
    private val triageApi: TriageApi,
) {
    suspend fun createSession(patientId: String, chiefComplaint: String? = null): TriageSessionDto =
        triageApi.createSession(CreateTriageSessionRequest(patientId = patientId, chiefComplaint = chiefComplaint))

    suspend fun getSession(sessionId: String): TriageSessionDto = triageApi.getSession(sessionId)

    suspend fun sendMessage(sessionId: String, message: String) {
        triageApi.sendMessage(sessionId, TriageMessageRequest(message))
    }

    suspend fun completeSession(sessionId: String): TriageSessionDto = triageApi.completeSession(sessionId)
}
