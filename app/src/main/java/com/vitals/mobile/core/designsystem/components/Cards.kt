package com.vitals.mobile.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vitals.mobile.core.designsystem.VitalsTheme

/** Soft bordered surface card used across screens. */
@Composable
fun VitalsCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = VitalsTheme.shapes.card,
    content: @Composable () -> Unit,
) {
    val colors = VitalsTheme.colors
    Box(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = shape,
                ambientColor = Color(0x1411442F),
                spotColor = Color(0x1411442F),
            )
            .clip(shape)
            .background(colors.surface)
            .border(BorderStroke(1.dp, colors.border.copy(alpha = 0.75f)), shape),
    ) {
        content()
    }
}

/** A tappable list row used on the "Ещё" menu and similar navigation lists. */
@Composable
fun VitalsNavRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VitalsTheme.colors
    VitalsCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = VitalsTheme.typography.titleSmall, color = colors.textPrimary)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textMuted.copy(alpha = 0.8f),
            )
        }
    }
}
