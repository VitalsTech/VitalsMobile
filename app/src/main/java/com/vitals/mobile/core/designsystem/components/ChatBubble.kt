package com.vitals.mobile.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
 * [fromMe] aligns the bubble to the right with the muted "own message" background;
 * incoming messages align left on the surface background.
 */
@Composable
fun ChatBubble(
    text: String,
    fromMe: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = VitalsTheme.colors
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (fromMe) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
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
    }
}
