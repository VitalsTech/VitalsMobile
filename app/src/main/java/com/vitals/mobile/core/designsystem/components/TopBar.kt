package com.vitals.mobile.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.vitals.mobile.core.designsystem.VitalsTheme

/** Top bar used on the 5 bottom-nav root screens: "Vitals" brand + role tag. */
@Composable
fun VitalsMainTopBar(
    modifier: Modifier = Modifier,
    roleLabel: String = "Пациент",
) {
    val colors = VitalsTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(colors.headerBackground, colors.headerBackground.copy(alpha = 0.94f)),
                ),
            )
            .statusBarsPadding()
            .height(52.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Vitals",
            style = VitalsTheme.typography.titleLarge,
            color = colors.onHeaderBackground,
        )
        Text(
            text = roleLabel,
            style = VitalsTheme.typography.bodySmall,
            color = colors.onHeaderMuted,
        )
    }
}

/** Top bar used on detail/sub screens: back button + title. */
@Composable
fun VitalsBackTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    val colors = VitalsTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.headerBackground)
            .statusBarsPadding()
            .height(52.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = colors.onHeaderBackground,
            )
        }
        Text(
            text = title,
            style = VitalsTheme.typography.titleMedium,
            color = colors.onHeaderBackground,
            modifier = Modifier.padding(start = 2.dp).weight(1f),
        )
        actions()
    }
}
