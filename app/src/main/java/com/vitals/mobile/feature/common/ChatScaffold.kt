package com.vitals.mobile.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.ChatBubble
import com.vitals.mobile.core.designsystem.components.VitalsSuggestionChip
import com.vitals.mobile.core.designsystem.components.VitalsTextField

data class UiChatMessage(
    val id: String,
    val text: String,
    val fromMe: Boolean,
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
) {
    val colors = VitalsTheme.colors
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

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
            itemsIndexed(messages, key = { _, item -> item.id }) { _, message ->
                ChatBubble(text = message.text, fromMe = message.fromMe)
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
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            VitalsTextField(
                value = inputText,
                onValueChange = onInputChange,
                placeholder = "Сообщение...",
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
