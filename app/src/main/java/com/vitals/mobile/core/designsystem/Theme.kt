package com.vitals.mobile.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

val LocalVitalsColors = staticCompositionLocalOf { LightVitalsColors }
val LocalVitalsTypography = compositionLocalOf { VitalsTypography }

object VitalsTheme {
    val colors: VitalsColorPalette
        @Composable get() = LocalVitalsColors.current

    val typography
        @Composable get() = LocalVitalsTypography.current

    val shapes = VitalsRadius
}

@Composable
fun VitalsAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkVitalsColors else LightVitalsColors

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            secondary = palette.accent,
            onSecondary = palette.onAccent,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceMuted,
            onSurfaceVariant = palette.textMuted,
            outline = palette.border,
            error = palette.danger,
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            secondary = palette.accent,
            onSecondary = palette.onAccent,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceMuted,
            onSurfaceVariant = palette.textMuted,
            outline = palette.border,
            error = palette.danger,
        )
    }

    CompositionLocalProvider(
        LocalVitalsColors provides palette,
        LocalVitalsTypography provides VitalsTypography,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VitalsTypography,
            shapes = VitalsShapes,
            content = content,
        )
    }
}
