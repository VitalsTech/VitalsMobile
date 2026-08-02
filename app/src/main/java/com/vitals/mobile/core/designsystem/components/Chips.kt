package com.vitals.mobile.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.vitals.mobile.core.designsystem.VitalsTheme

/** Rounded pill chip used for AI-chat quick replies. */
@Composable
fun VitalsSuggestionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VitalsTheme.colors
    Text(
        text = text,
        style = VitalsTheme.typography.bodySmall,
        color = colors.textPrimary,
        modifier = modifier
            .clip(VitalsTheme.shapes.chip)
            .background(colors.surface)
            .border(1.dp, colors.border, VitalsTheme.shapes.chip)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}

enum class StatusTone { NEUTRAL, SUCCESS, WARNING, DANGER, ACCENT }

/** Small status/severity badge, e.g. document type, order status, consultation state. */
@Composable
fun VitalsStatusChip(
    text: String,
    tone: StatusTone = StatusTone.NEUTRAL,
    modifier: Modifier = Modifier,
) {
    val colors = VitalsTheme.colors
    val (bg, fg) = when (tone) {
        StatusTone.NEUTRAL -> colors.surfaceMuted to colors.textMuted
        StatusTone.SUCCESS -> colors.success.copy(alpha = 0.15f) to colors.success
        StatusTone.WARNING -> colors.warning.copy(alpha = 0.15f) to colors.warning
        StatusTone.DANGER -> colors.danger.copy(alpha = 0.15f) to colors.danger
        StatusTone.ACCENT -> colors.accent.copy(alpha = 0.2f) to colors.textPrimary
    }
    Text(
        text = text,
        style = VitalsTheme.typography.labelMedium,
        color = fg,
        modifier = modifier
            .clip(VitalsTheme.shapes.chip)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
