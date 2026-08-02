package com.vitals.mobile.core.data.prescriptions

import kotlinx.serialization.Serializable

enum class PrescriptionStatus(val wireValue: String) {
    DRAFT("draft"),
    SIGNED("signed"),
    SENT_TO_PHARMACY("sent_to_pharmacy"),
    PARTIALLY_FULFILLED("partially_fulfilled"),
    FULFILLED("fulfilled"),
    DISPENSED("dispensed"),
    EXPIRED("expired"),
    CANCELLED("cancelled");

    companion object {
        fun fromWire(value: String?): PrescriptionStatus? = entries.firstOrNull { it.wireValue == value }
    }
}

@Serializable
data class MedicationItem(
    val name: String,
    val dosage: String? = null,
    val frequency: String? = null,
    val durationDays: Int? = null,
    val instructions: String? = null,
)

@Serializable
data class CreatePrescriptionRequest(
    val patientId: String,
    val consultationId: String? = null,
    val medications: List<MedicationItem>,
)

@Serializable
data class SendToPharmacyRequest(
    val pharmacyId: String? = null,
    val autoSelectNearest: Boolean = true,
)

@Serializable
data class CancelPrescriptionRequest(val reason: String? = null)

@Serializable
data class PrescriptionDto(
    val id: String? = null,
    val patientId: String? = null,
    val doctorId: String? = null,
    val status: String? = null,
    val medications: List<MedicationItem>? = null,
    val createdAt: String? = null,
    val signedAt: String? = null,
) {
    val resolvedStatus: PrescriptionStatus? get() = PrescriptionStatus.fromWire(status)
}
