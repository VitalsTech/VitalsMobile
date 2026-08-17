package com.vitals.mobile.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.vitals.mobile.core.designsystem.VitalsTheme

/**
 * A single chat message bubble used in triage / AI-assistant / doctor chats.
 * [fromMe] aligns the bubble to the right; shows time and delivery status like web ChatBubble.
 */
@Composable
fun ChatBubble(
    text: String,
    fromMe: Boolean,
    modifier: Modifier = Modifier,
    timeLabel: String? = null,
    statusLabel: String? = null,
    isSystem: Boolean = false,
) {
    val colors = VitalsTheme.colors
    if (isSystem) {
        Column(
            modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = text,
                style = VitalsTheme.typography.labelMedium,
                color = colors.textMuted,
            )
            if (!timeLabel.isNullOrBlank()) {
                Text(
                    text = timeLabel,
                    style = VitalsTheme.typography.labelMedium,
                    color = colors.textMuted,
                )
            }
        }
        return
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (fromMe) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(VitalsTheme.shapes.input)
                .background(if (fromMe) colors.background else colors.surfaceMuted)
                .border(1.dp, colors.border, VitalsTheme.shapes.input)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = text,
                style = VitalsTheme.typography.bodySmall,
                color = colors.textPrimary,
            )
        }
        if (!timeLabel.isNullOrBlank() || !statusLabel.isNullOrBlank()) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Like web: own messages show «Прочитано/Доставлено» + time.
                if (fromMe && !statusLabel.isNullOrBlank()) {
                    Text(
                        text = statusLabel,
                        style = VitalsTheme.typography.labelMedium,
                        color = colors.textMuted,
                    )
                }
                if (!timeLabel.isNullOrBlank()) {
                    Text(
                        text = timeLabel,
                        style = VitalsTheme.typography.labelMedium,
                        color = colors.textMuted,
                    )
                }
            }
        }
    }
}
