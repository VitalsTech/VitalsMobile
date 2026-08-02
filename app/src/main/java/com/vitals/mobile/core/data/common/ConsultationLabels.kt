package com.vitals.mobile.core.data.common

/** RU labels for consultation wire statuses / types. */
object ConsultationLabels {
    fun status(raw: String?): String = when (raw?.trim()?.lowercase()) {
        null, "" -> ""
        "scheduled", "booked", "bookedpendingsession" -> "Запланирована"
        "waiting", "pending" -> "Ожидание"
        "active", "inprogress", "in_progress", "open" -> "Идёт"
        "completed", "complete" -> "Завершена"
        "doctorleft", "doctor_left" -> "Врач завершил"
        "patientleft", "patient_left" -> "Пациент вышел"
        "cancelled", "canceled" -> "Отменена"
        "expired" -> "Истекла"
        else -> raw.trim()
    }

    fun type(raw: String?): String = when (raw?.trim()?.lowercase()) {
        null, "" -> "Консультация"
        "syncchat", "sync_chat", "chat" -> "Чат"
        "video" -> "Видео"
        "async" -> "Асинхронный чат"
        "inperson", "in_person", "offline" -> "Очно"
        "homevisit", "home_visit" -> "Вызов на дом"
        else -> "Консультация"
    }

    fun isTerminal(raw: String?): Boolean {
        val key = raw?.trim()?.lowercase().orEmpty()
        return key in setOf(
            "completed", "complete", "cancelled", "canceled",
            "expired", "doctorleft", "doctor_left",
        )
    }
}
