package com.vitals.mobile.core.data.medicalrecords

import com.vitals.mobile.core.network.asArrayFlexible
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
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
        return normalizeHistoryEvents(raw)
    }

    /** History + optional currentState from `{ events, currentState }` envelope. */
    suspend fun getHistoryBundle(patientId: String): Pair<List<MedicalRecordEventDto>, PatientStateDto?> {
        val raw = api.getHistory(patientId)
        val events = normalizeHistoryEvents(raw)
        val state = (raw as? JsonObject)?.get("currentState")?.let { parsePatientState(it) }
        return events to state
    }

    suspend fun getState(patientId: String): PatientStateDto? =
        runCatching { parsePatientState(api.getState(patientId)) }.getOrNull()

    /** profileId + publicId - как web `getStateAliases`. */
    suspend fun getStateAliases(patientIds: Collection<String>): PatientStateDto? {
        val unique = patientIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        for (id in unique) {
            val state = runCatching { getState(id) }.getOrNull()
            if (state != null) return state
        }
        return null
    }

    suspend fun getHistoryAliases(
        patientIds: Collection<String>,
        eventTypes: List<String>? = null,
    ): List<MedicalRecordEventDto> {
        val unique = patientIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        for (id in unique) {
            val history = runCatching { getHistory(id, eventTypes) }.getOrNull()
            if (!history.isNullOrEmpty()) return history
            if (history != null) return history
        }
        return emptyList()
    }

    suspend fun getHistoryBundleAliases(
        patientIds: Collection<String>,
    ): Pair<List<MedicalRecordEventDto>, PatientStateDto?> {
        val unique = patientIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        for (id in unique) {
            val bundle = runCatching { getHistoryBundle(id) }.getOrNull() ?: continue
            if (bundle.first.isNotEmpty() || bundle.second != null) return bundle
        }
        return emptyList<MedicalRecordEventDto>() to null
    }

    fun diagnosisLabelFromEvent(event: MedicalRecordEventDto): String? {
        val type = event.eventType.orEmpty()
        if (!type.equals("diagnosis", true) && !type.equals("DiagnosisConfirmed", true)) return null
        val map = parsePayloadMap(event)
        val code = map["icd10Code"] ?: map["code"]
        val title = map["description"] ?: map["title"]
        return when {
            !code.isNullOrBlank() && !title.isNullOrBlank() -> "$code - $title"
            !title.isNullOrBlank() -> title
            !code.isNullOrBlank() -> code
            else -> null
        }
    }

    fun parsePayloadMap(event: MedicalRecordEventDto): Map<String, String> {
        val raw: JsonElement? = when {
            event.payload != null -> event.payload
            !event.payloadJson.isNullOrBlank() -> runCatching { json.parseToJsonElement(event.payloadJson) }.getOrNull()
            else -> null
        }
        val obj = raw as? JsonObject ?: return emptyMap()
        return obj.mapNotNull { (key, value) ->
            val text = (value as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
            if (text.isBlank()) null else key to text
        }.toMap()
    }

    private fun normalizeHistoryEvents(raw: JsonElement): List<MedicalRecordEventDto> {
        return raw.asArrayFlexible("items", "events", "history").mapNotNull { element ->
            runCatching { json.decodeFromJsonElement<MedicalRecordEventDto>(element) }.getOrNull()
        }
    }

    /**
     * Flexible state parser: `activeDiagnoses` may be objects or strings like `"пр - апрол"`.
     */
    private fun parsePatientState(raw: JsonElement): PatientStateDto? {
        val obj = raw as? JsonObject ?: return null
        fun str(key: String): String? = obj[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
        fun stringList(key: String): List<String> {
            val el = obj[key] ?: return emptyList()
            return when (el) {
                is JsonArray -> el.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { s -> s.isNotEmpty() } }
                is JsonPrimitive -> el.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }?.let { listOf(it) }.orEmpty()
                else -> emptyList()
            }
        }

        val diagnoses = parseDiagnoses(obj["activeDiagnoses"])
        return PatientStateDto(
            patientId = str("patientId"),
            summary = str("summary"),
            allergies = str("allergies") ?: stringList("allergies").joinToString(", ").ifBlank { null },
            bloodType = str("bloodType"),
            activeConditions = stringList("activeConditions"),
            activeDiagnoses = diagnoses,
            activeMedications = stringList("activeMedications"),
            recentLabResults = stringList("recentLabResults"),
            lastVisitAt = str("lastVisitAt"),
        )
    }

    private fun parseDiagnoses(raw: JsonElement?): List<DiagnosisDto> {
        if (raw == null) return emptyList()
        val array = when (raw) {
            is JsonArray -> raw
            else -> return emptyList()
        }
        return array.mapIndexedNotNull { index, el ->
            when (el) {
                is JsonPrimitive -> {
                    val text = el.contentOrNull?.trim().orEmpty()
                    if (text.isEmpty()) null
                    else {
                        val parts = text.split("—", " - ", limit = 2).map { it.trim() }
                        if (parts.size == 2) {
                            DiagnosisDto(code = parts[0], title = parts[1], sourceEventId = "str-$index")
                        } else {
                            DiagnosisDto(title = text, sourceEventId = "str-$index")
                        }
                    }
                }
                is JsonObject -> DiagnosisDto(
                    icd10Code = el["icd10Code"]?.jsonPrimitive?.contentOrNull,
                    code = el["code"]?.jsonPrimitive?.contentOrNull,
                    description = el["description"]?.jsonPrimitive?.contentOrNull,
                    title = el["title"]?.jsonPrimitive?.contentOrNull,
                    recordedAt = el["recordedAt"]?.jsonPrimitive?.contentOrNull,
                    occurredAt = el["occurredAt"]?.jsonPrimitive?.contentOrNull,
                    sourceEventId = el["sourceEventId"]?.jsonPrimitive?.contentOrNull ?: "obj-$index",
                )
                else -> null
            }
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
