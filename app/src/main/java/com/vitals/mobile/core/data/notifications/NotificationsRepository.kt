package com.vitals.mobile.core.data.notifications

import com.vitals.mobile.core.network.asArrayFlexible
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationsRepository @Inject constructor(
    private val api: NotificationsApi,
    private val json: Json,
) {
    suspend fun getHistory(limit: Int? = null): List<NotificationDto> {
        val raw = api.getHistory(limit)
        return raw.asArrayFlexible("items", "data", "notifications").mapNotNull { element ->
            runCatching { json.decodeFromJsonElement<NotificationDto>(element) }.getOrNull()
        }
    }

    suspend fun registerPushToken(token: String) {
        runCatching { api.registerPushToken(RegisterPushTokenRequest(token = token)) }
    }
}
