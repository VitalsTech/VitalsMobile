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
        fun normalize(value: String?): String {
            if (value.isNullOrBlank()) return ""
            return value
                .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
                .replace('-', '_')
                .lowercase()
        }

        fun fromWire(value: String?): PrescriptionStatus? {
            val normalized = normalize(value)
            return entries.firstOrNull { it.wireValue == normalized }
        }
    }
}

@Serializable
data class MedicationItem(
    val name: String? = null,
    val tradeName: String? = null,
    val inn: String? = null,
    val dosageForm: String? = null,
    val dosage: String? = null,
    val frequency: String? = null,
    val durationDays: Int? = null,
    val courseDays: Int? = null,
    val packageQuantity: String? = null,
    val instructions: String? = null,
    val specialInstructions: String? = null,
) {
    val resolvedName: String
        get() = tradeName?.trim()?.takeIf { it.isNotEmpty() }
            ?: name?.trim()?.takeIf { it.isNotEmpty() }
            ?: inn?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Препарат"

    fun schemeLine(): String = listOfNotNull(
        inn?.takeIf { it.isNotBlank() && !it.equals(tradeName, true) }?.let { "МНН: $it" },
        dosageForm?.takeIf { it.isNotBlank() },
        dosage?.takeIf { it.isNotBlank() },
        frequency?.takeIf { it.isNotBlank() },
        (courseDays ?: durationDays)?.let { "$it дн." },
        packageQuantity?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")
}

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
    val prescriptionId: String? = null,
    val patientId: String? = null,
    val doctorId: String? = null,
    val status: String? = null,
    val diagnosisForPrescription: String? = null,
    val medications: List<MedicationItem>? = null,
    val createdAt: String? = null,
    val signedAt: String? = null,
    val sentToPharmacyAt: String? = null,
    val validUntil: String? = null,
) {
    val resolvedId: String get() = id ?: prescriptionId.orEmpty()

    val resolvedStatus: PrescriptionStatus? get() = PrescriptionStatus.fromWire(status)

    fun formatMedications(): String {
        val names = medications.orEmpty().map { it.resolvedName }.filter { it.isNotBlank() }
        return if (names.isNotEmpty()) names.joinToString(", ") else "Рецепт"
    }

    fun displayTitle(): String =
        medications?.firstOrNull()?.resolvedName
            ?: diagnosisForPrescription?.trim()?.takeIf { it.isNotEmpty() }
            ?: resolvedId.take(8).takeIf { it.isNotEmpty() }?.let { "Рецепт №$it" }
            ?: "Рецепт"

    fun canShowQr(): Boolean {
        val key = PrescriptionStatus.normalize(status)
        return key.isNotEmpty() && key != "draft" && key != "cancelled" && key != "canceled" && key != "expired"
    }

    fun canSendToPharmacy(): Boolean = PrescriptionStatus.normalize(status) == "signed"

    fun canCancel(): Boolean {
        val key = PrescriptionStatus.normalize(status)
        return key in setOf("draft", "signed", "sent_to_pharmacy")
    }
}
