package com.vitals.mobile.core.data.common

import com.vitals.mobile.core.data.routing.RoutingStepDto

/** Mirrors VitalsWeb `routeStepTitle` / status labels. */
object RouteLabels {
    private val ACTION_TITLES = mapOf(
        "lab.order" to "Анализы",
        "lab" to "Анализы",
        "consultation" to "Консультация врача",
        "house.call" to "Вызов на дом",
        "emergency" to "Экстренная помощь",
    )

    private val STATUS_LABELS = mapOf(
        "pending" to "Ожидает",
        "waiting" to "Ожидает",
        "current" to "Текущий",
        "active" to "Текущий",
        "in_progress" to "В процессе",
        "inprogress" to "В процессе",
        "done" to "Выполнено",
        "completed" to "Выполнено",
        "skipped" to "Пропущен",
        "cancelled" to "Отменён",
    )

    fun stepTitle(step: RoutingStepDto, index: Int = 0): String {
        val titled = step.title?.trim().orEmpty()
        if (titled.isNotEmpty()) return titled
        val labeled = step.label?.trim().orEmpty()
        if (labeled.isNotEmpty() && !labeled.equals(step.action, ignoreCase = true)) return labeled
        val action = (step.action ?: step.type).orEmpty().lowercase()
        ACTION_TITLES[action]?.let { return it }
        return "Шаг ${step.stepNumber ?: index + 1}"
    }

    fun stepStatus(raw: String?): String {
        val key = raw?.trim()?.lowercase().orEmpty()
        if (key.isEmpty()) return ""
        return STATUS_LABELS[key] ?: raw!!.trim()
    }

    fun isStepDone(raw: String?): Boolean {
        val key = raw?.trim()?.lowercase().orEmpty()
        return key == "done" || key == "completed"
    }

    fun isLabAction(step: RoutingStepDto): Boolean {
        val action = (step.action ?: step.type).orEmpty().lowercase()
        return action == "lab.order" || action == "lab"
    }

    fun isConsultationAction(step: RoutingStepDto): Boolean {
        val action = (step.action ?: step.type).orEmpty().lowercase()
        return action == "consultation"
    }
}
