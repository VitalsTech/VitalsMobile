package com.vitals.mobile.core.data.doctors

import com.vitals.mobile.core.network.asArrayFlexible
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DoctorsRepository @Inject constructor(
    private val doctorsApi: DoctorsApi,
    private val json: Json,
) {
    suspend fun listDoctors(specialization: String? = null, query: String? = null): List<DoctorDto> {
        val raw = doctorsApi.listDoctors(specialization = specialization, query = query)
        return raw.asArrayFlexible().mapNotNull { element ->
            runCatching { json.decodeFromJsonElement<DoctorDto>(element) }.getOrNull()
        }
    }

    suspend fun getDoctor(doctorId: String): DoctorDto = doctorsApi.getDoctor(doctorId)

    suspend fun getSchedule(doctorId: String, from: String? = null, days: Int? = null): List<ScheduleSlotDto> {
        val raw = doctorsApi.getSchedule(doctorId, from, days)
        return raw.asArrayFlexible("items", "slots", "data").mapNotNull { element ->
            runCatching { json.decodeFromJsonElement<ScheduleSlotDto>(element) }.getOrNull()
        }
    }
}
