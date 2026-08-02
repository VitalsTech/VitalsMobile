package com.vitals.mobile.core.data.notifications

import kotlinx.serialization.Serializable

@Serializable
data class RegisterPushTokenRequest(
    val platform: String = "android",
    val token: String,
)

@Serializable
data class UserPreferenceDto(
    val bookingEnabled: Boolean? = null,
    val triageEnabled: Boolean? = null,
    val moodEnabled: Boolean? = null,
    val messageEnabled: Boolean? = null,
    val scheduleEnabled: Boolean? = null,
)

/** Categories used across the UI: booking, triage, mood, message/messages, schedule. */
@Serializable
data class NotificationDto(
    val id: String? = null,
    val category: String? = null,
    val title: String? = null,
    val body: String? = null,
    val message: String? = null,
    val isRead: Boolean? = null,
    val createdAt: String? = null,
) {
    val resolvedBody: String get() = body ?: message.orEmpty()
}
