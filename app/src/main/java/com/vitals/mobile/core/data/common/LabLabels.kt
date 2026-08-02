package com.vitals.mobile.core.data.common

object LabLabels {
    fun orderStatus(raw: String?): String = when (raw?.trim()?.lowercase()) {
        null, "" -> "В ожидании"
        "ordered", "назначено", "assigned" -> "Назначено"
        "inprogress", "in_progress", "processing" -> "В работе"
        "completed", "done" -> "Готово"
        "cancelled", "canceled" -> "Отменено"
        else -> raw.trim()
    }

    fun prescriptionStatus(raw: String?): String = when (raw?.trim()?.lowercase()) {
        null, "" -> ""
        "draft" -> "Черновик"
        "signed" -> "Подписан"
        "sent_to_pharmacy", "senttopharmacy" -> "В аптеке"
        "partially_fulfilled", "partiallyfulfilled" -> "Частично выдан"
        "fulfilled", "dispensed" -> "Выдан"
        "expired" -> "Истёк"
        "cancelled", "canceled" -> "Отменён"
        else -> raw.trim()
    }
}
