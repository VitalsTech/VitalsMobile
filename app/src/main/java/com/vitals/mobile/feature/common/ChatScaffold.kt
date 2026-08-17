package com.vitals.mobile.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.vitals.mobile.core.data.common.ChatMessageDto
import com.vitals.mobile.core.data.common.ChatMessageLabels
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.ChatBubble
import com.vitals.mobile.core.designsystem.components.VitalsSuggestionChip
import com.vitals.mobile.core.designsystem.components.VitalsTextField
import java.time.Instant
import kotlinx.coroutines.flow.first

data class UiChatMessage(
    val id: String,
    val text: String,
    val fromMe: Boolean,
    val sentAt: String? = null,
    val readAt: String? = null,
) {
    val timeLabel: String get() = ChatMessageLabels.formatTime(sentAt)
    val statusLabel: String? get() = ChatMessageLabels.deliveryStatus(fromMe, readAt)
}

fun ChatMessageDto.toUiChatMessage(): UiChatMessage = UiChatMessage(
    id = resolvedId,
    text = resolvedText,
    fromMe = isFromCurrentUser,
    sentAt = resolvedSentAt,
    readAt = readAt,
)

fun optimisticUiChatMessage(text: String): UiChatMessage = UiChatMessage(
    id = "local-${System.nanoTime()}",
    text = text,
    fromMe = true,
    sentAt = Instant.now().toString(),
    readAt = null,
)

/**
 * Shared chat layout (message list + optional quick-reply chips + composer) used by the AI
 * assistant / triage onboarding chat and the doctor chat screens.
 */
@Composable
fun ChatBody(
    messages: List<UiChatMessage>,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    quickReplies: List<String> = emptyList(),
    onQuickReply: (String) -> Unit = {},
    sendEnabled: Boolean = true,
    emptyPlaceholder: String? = "Опишите жалобу - ИИ задаст уточняющие вопросы",
    inputPlaceholder: String = "Сообщение...",
    isThinking: Boolean = false,
    thinkingLabel: String = "ИИ печатает…",
) {
    val colors = VitalsTheme.colors
    val listState = rememberLazyListState()
    val lastMessageId = messages.lastOrNull()?.id
    val lastMessageReadAt = messages.lastOrNull()?.readAt

    // Scroll to bottom on new/updated messages or "typing" indicator (like web useChatAutoScroll).
    LaunchedEffect(lastMessageId, messages.size, lastMessageReadAt, isThinking) {
        val lastIndex = when {
            messages.isNotEmpty() -> if (isThinking) messages.size else messages.size - 1
            isThinking -> 0
            !emptyPlaceholder.isNullOrBlank() -> 0
            else -> return@LaunchedEffect
        }
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .first { it > lastIndex }
        listState.animateScrollToItem(lastIndex)
    }

    // IME padding is applied once on NavHost in MainScaffold - not here.
    Column(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(VitalsTheme.shapes.card)
                .background(colors.surface)
                .border(1.dp, colors.border, VitalsTheme.shapes.card)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (messages.isEmpty() && !emptyPlaceholder.isNullOrBlank() && !isThinking) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = emptyPlaceholder,
                            style = VitalsTheme.typography.bodyMedium,
                            color = colors.textMuted,
                        )
                    }
                }
            }
            itemsIndexed(messages, key = { _, item -> item.id }) { _, message ->
                ChatBubble(
                    text = message.text,
                    fromMe = message.fromMe,
                    timeLabel = message.timeLabel.takeIf { it.isNotBlank() },
                    statusLabel = message.statusLabel,
                )
            }
            if (isThinking) {
                item(key = "thinking") {
                    Text(
                        text = thinkingLabel,
                        style = VitalsTheme.typography.bodySmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    )
                }
            }
        }

        if (quickReplies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickReplies.forEach { reply ->
                    VitalsSuggestionChip(text = reply, onClick = { onQuickReply(reply) })
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            VitalsTextField(
                value = inputText,
                onValueChange = onInputChange,
                placeholder = inputPlaceholder,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(10.dp))
            IconButton(
                onClick = onSend,
                enabled = sendEnabled,
                modifier = Modifier
                    .size(48.dp)
                    .clip(VitalsTheme.shapes.button)
                    .background(colors.primary),
                colors = IconButtonDefaults.iconButtonColors(contentColor = colors.onPrimary),
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
            }
        }
    }
}
