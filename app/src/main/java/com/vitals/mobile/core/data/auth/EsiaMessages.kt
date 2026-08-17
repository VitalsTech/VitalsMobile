package com.vitals.mobile.core.data.auth

import retrofit2.HttpException

/** User-facing ESIA / Госуслуги errors from the frontend TZ. */
object EsiaMessages {
    const val DISABLED = "Вход через Госуслуги на этой среде выключен"
    const val EXISTING_ACCOUNT = "Вошли в существующий аккаунт с этим телефоном"
    const val GENERIC = "Не удалось войти через Госуслуги"

    fun map(throwable: Throwable): String {
        val http = throwable as? HttpException
        if (http?.code() == 503) return DISABLED
        val message = throwable.message.orEmpty()
        return when {
            message.contains("503") ||
                message.contains("not configured", ignoreCase = true) ||
                message.contains("выключен") -> DISABLED
            message.isNotBlank() -> message
            else -> GENERIC
        }
    }
}
