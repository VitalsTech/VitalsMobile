package com.vitals.mobile.core.data.common

import kotlinx.serialization.Serializable

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
    val createdAt: String? = null,
    val timestamp: String? = null,
) {
    val resolvedId: String get() = id ?: messageId ?: (sequence?.toString()) ?: hashCode().toString()

    val resolvedText: String get() = content ?: message ?: text.orEmpty()

    val isFromCurrentUser: Boolean
        get() {
            val roleValue = (role ?: sender ?: senderRole)?.lowercase().orEmpty()
            return roleValue.contains("patient") || roleValue.contains("user") || roleValue == "me"
        }
}
