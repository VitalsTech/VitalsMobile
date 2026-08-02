package com.vitals.mobile.core.data.doctors

import com.vitals.mobile.core.data.common.DoctorLabels
import kotlinx.serialization.Serializable

@Serializable
data class DoctorDto(
    val id: String? = null,
    val doctorId: String? = null,
    val publicId: String? = null,
    val firstName: String? = null,
    val secondName: String? = null,
    val surename: String? = null,
    val fullName: String? = null,
    val name: String? = null,
    val specialization: String? = null,
    val specialty: String? = null,
    val academicDegree: String? = null,
    val biography: String? = null,
    val bio: String? = null,
    val description: String? = null,
    val licenseNumber: String? = null,
    val rating: Double? = null,
) {
    val resolvedId: String get() = id ?: doctorId ?: publicId.orEmpty()

    val displayName: String get() = DoctorLabels.formatName(this)

    val displaySpecialty: String get() = DoctorLabels.formatSpecialty(this)
}

@Serializable
data class ScheduleSlotDto(
    val id: String? = null,
    val slotId: String? = null,
    val doctorId: String? = null,
    val startsAt: String? = null,
    val startAt: String? = null,
    val startTime: String? = null,
    val start: String? = null,
    val endsAt: String? = null,
    val endAt: String? = null,
    val endTime: String? = null,
    val end: String? = null,
    val isBooked: Boolean? = null,
    val isAvailable: Boolean? = null,
    val available: Boolean? = null,
    val isOnline: Boolean? = null,
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
