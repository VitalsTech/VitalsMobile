package com.vitals.mobile.core.data.medicalrecords

import com.vitals.mobile.core.network.asArrayFlexible
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicalRecordsRepository @Inject constructor(
    private val api: MedicalRecordsApi,
    private val json: Json,
) {
    suspend fun recordMoodCheck(patientId: String, mood: MoodCode) {
        val payload = MoodCheckPayload(
            mood = mood.label,
            moodCode = mood.wireValue,
            severity = mood.severity,
            notifyDoctor = true,
            escalate = mood == MoodCode.WORSE,
        )
        appendEvent(patientId, MedicalRecordEventTypes.MOOD_CHECK, json.encodeToString(payload))
    }

    suspend fun recordDocument(patientId: String, title: String, docType: String, hasPaper: Boolean, hasDigital: Boolean) {
        val payload = DocumentEventPayload(title, docType, hasPaper, hasDigital)
        appendEvent(patientId, MedicalRecordEventTypes.DOCUMENT, json.encodeToString(payload))
    }

    suspend fun recordSupportRequest(patientId: String, topic: String, message: String) {
        val payload = SupportRequestPayload(topic, message)
        appendEvent(patientId, MedicalRecordEventTypes.SUPPORT_REQUEST, json.encodeToString(payload))
    }

    suspend fun recordHouseCallRequest(
        patientId: String,
        address: String,
        symptoms: String,
        desiredTime: String?,
        phone: String?,
        urgent: Boolean,
    ) {
        val payload = HouseCallRequestPayload(address, symptoms, desiredTime, phone, urgent)
        appendEvent(patientId, MedicalRecordEventTypes.HOUSE_CALL_REQUEST, json.encodeToString(payload))
    }

    suspend fun getHistory(patientId: String, eventTypes: List<String>? = null): List<MedicalRecordEventDto> {
        val raw = api.getHistory(patientId, eventTypes = eventTypes?.joinToString(","))
        return raw.asArrayFlexible("items", "events", "history").mapNotNull { element ->
            runCatching { json.decodeFromJsonElement<MedicalRecordEventDto>(element) }.getOrNull()
        }
    }

    private suspend fun appendEvent(patientId: String, eventType: String, payloadJson: String) {
        api.appendEvent(
            patientId,
            AppendEventRequest(
                eventType = eventType,
                payloadJson = payloadJson,
                occurredAt = Instant.now().toString(),
            ),
        )
    }
}
