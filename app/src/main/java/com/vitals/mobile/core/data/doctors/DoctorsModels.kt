package com.vitals.mobile.core.data.doctors

import kotlinx.serialization.Serializable

@Serializable
data class DoctorDto(
    val id: String? = null,
    val doctorId: String? = null,
    val publicId: String? = null,
    val firstName: String? = null,
    val secondName: String? = null,
    val surename: String? = null,
    val specialization: String? = null,
    val academicDegree: String? = null,
    val biography: String? = null,
    val licenseNumber: String? = null,
    val rating: Double? = null,
) {
    val resolvedId: String get() = id ?: doctorId ?: publicId.orEmpty()

    val fullName: String
        get() = listOfNotNull(secondName, firstName, surename).filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { "Врач" }
}

@Serializable
data class ScheduleSlotDto(
    val id: String? = null,
    val slotId: String? = null,
    val doctorId: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val isBooked: Boolean? = null,
    val status: String? = null,
) {
    val resolvedId: String get() = id ?: slotId.orEmpty()
}

@Serializable
data class DoctorScheduleSlotPayload(
    val startTime: String,
    val endTime: String,
)

@Serializable
data class DoctorProfileUpdateRequest(
    val biography: String? = null,
    val specialization: String? = null,
    val academicDegree: String? = null,
)
