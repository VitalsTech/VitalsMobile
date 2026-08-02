package com.vitals.mobile.core.data.laborders

import com.vitals.mobile.core.network.asArrayFlexible
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LabOrdersRepository @Inject constructor(
    private val api: LabOrdersApi,
    private val json: Json,
) {
    suspend fun getForPatient(patientId: String): List<LabOrderDto> {
        val raw = api.getForPatient(patientId)
        return raw.asArrayFlexible("items", "data", "orders").mapNotNull { element ->
            runCatching { json.decodeFromJsonElement<LabOrderDto>(element) }.getOrNull()
        }
    }
}
