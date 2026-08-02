package com.vitals.mobile.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vitals.mobile.core.designsystem.VitalsTheme

@Composable
fun VitalsPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = VitalsTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        enabled = enabled && !loading,
        shape = VitalsTheme.shapes.button,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.primary.copy(alpha = 0.38f),
            disabledContentColor = colors.onPrimary.copy(alpha = 0.7f),
        ),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        if (loading) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = colors.onPrimary,
                    strokeWidth = 2.dp,
                )
            }
        } else {
            Text(text = text, style = VitalsTheme.typography.labelLarge)
        }
    }
}

@Composable
fun VitalsSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = VitalsTheme.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        enabled = enabled,
        shape = VitalsTheme.shapes.button,
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.85f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface.copy(alpha = 0.9f),
            contentColor = colors.textPrimary,
            disabledContentColor = colors.textMuted,
        ),
    ) {
        Text(text = text, style = VitalsTheme.typography.labelLarge)
    }
}
