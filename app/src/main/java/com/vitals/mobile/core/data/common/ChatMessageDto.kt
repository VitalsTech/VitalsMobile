package com.vitals.mobile.core.data.common

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shared shape for chat-like messages across triage sessions, consultation chats, and the
 * AI-assistant. Backend field names vary by endpoint, so most fields are optional and the UI
 * layer resolves the best-effort role/content via the helper properties below.
 */
@Serializable
data class ChatMessageDto(
    val id: String? = null,
    val messageId: String? = null,
    val sequence: Long? = null,
    val role: String? = null,
    val sender: String? = null,
    val senderRole: String? = null,
    val messageType: String? = null,
    val message: String? = null,
    val content: String? = null,
    val text: String? = null,
    val attachmentUrl: String? = null,
    val isImportant: Boolean? = null,
    val sentAt: String? = null,
    val createdAt: String? = null,
    val timestamp: String? = null,
    val readAt: String? = null,
) {
    val resolvedId: String get() = id ?: messageId ?: (sequence?.toString()) ?: hashCode().toString()

    val resolvedText: String get() = content ?: message ?: text.orEmpty()

    /** Prefer sentAt, then createdAt / timestamp — mirrors web `toChatMessage`. */
    val resolvedSentAt: String? get() = sentAt ?: createdAt ?: timestamp

    val isFromCurrentUser: Boolean
        get() {
            val roleValue = (role ?: sender ?: senderRole)?.lowercase().orEmpty()
            return roleValue.contains("patient") || roleValue.contains("user") || roleValue == "me"
        }
}

/** RU chat time + delivery status, mirrors VitalsWeb `formatChatTime` / ChatBubble. */
object ChatMessageLabels {
    private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm", Locale("ru"))

    fun formatTime(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        val dateTime = parse(iso) ?: return ""
        return dateTime.format(TIME_FMT)
    }

    fun deliveryStatus(fromMe: Boolean, readAt: String?): String? {
        if (!fromMe) return null
        return if (!readAt.isNullOrBlank()) "Прочитано" else "Доставлено"
    }

    private fun parse(raw: String): LocalDateTime? {
        return runCatching { OffsetDateTime.parse(raw).toLocalDateTime() }.getOrNull()
            ?: runCatching { Instant.parse(raw).atZone(ZoneId.systemDefault()).toLocalDateTime() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(raw) }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(raw.substringBefore('.').replace(' ', 'T'))
            }.getOrNull()
    }
}
