package com.vitals.mobile.core.data.prescriptions

import com.vitals.mobile.core.network.asArrayFlexible
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrescriptionsRepository @Inject constructor(
    private val api: PrescriptionsApi,
    private val json: Json,
) {
    suspend fun getForPatient(patientId: String): List<PrescriptionDto> {
        val raw = api.getForPatient(patientId)
        return raw.asArrayFlexible("items", "data", "prescriptions").mapNotNull { element ->
            runCatching { json.decodeFromJsonElement<PrescriptionDto>(element) }.getOrNull()
        }
    }

    suspend fun get(id: String): PrescriptionDto = api.get(id)
}
