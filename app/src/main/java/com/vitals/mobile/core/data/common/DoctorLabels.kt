package com.vitals.mobile.core.data.common

import com.vitals.mobile.core.data.doctors.DoctorDto

/** Mirrors VitalsWeb `formatDoctorName` / specialty display. */
object DoctorLabels {
    private val SPECIALTY_RU = mapOf(
        "therapist" to "Терапевт",
        "general practitioner" to "Терапевт",
        "gp" to "Терапевт",
        "cardiologist" to "Кардиолог",
        "neurologist" to "Невролог",
        "pediatrician" to "Педиатр",
        "surgeon" to "Хирург",
        "dermatologist" to "Дерматолог",
        "ophthalmologist" to "Офтальмолог",
        "otolaryngologist" to "ЛОР",
        "ent" to "ЛОР",
        "gynecologist" to "Гинеколог",
        "urologist" to "Уролог",
        "endocrinologist" to "Эндокринолог",
        "psychiatrist" to "Психиатр",
        "psychologist" to "Психолог",
    )

    fun formatName(doctor: DoctorDto?): String {
        if (doctor == null) return "Врач Vitals"
        doctor.fullName?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        doctor.name?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        val parts = listOfNotNull(doctor.surename, doctor.firstName, doctor.secondName)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return if (parts.isNotEmpty()) parts.joinToString(" ") else "Врач Vitals"
    }

    fun formatSpecialty(doctor: DoctorDto?): String {
        val raw = doctor?.specialization?.trim()?.takeIf { it.isNotEmpty() }
            ?: doctor?.specialty?.trim()?.takeIf { it.isNotEmpty() }
            ?: return "Специализация не указана"
        return SPECIALTY_RU[raw.lowercase()] ?: raw
    }
}
