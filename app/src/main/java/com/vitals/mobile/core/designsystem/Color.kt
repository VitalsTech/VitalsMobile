package com.vitals.mobile.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Semantic color palette for the Vitals patient app.
 * Values mirror the design tokens used by VitalsWeb (`src/index.css`) and the Figma mobile mockups,
 * so light/dark parity matches the web portal.
 */
data class VitalsColorPalette(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val border: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val onAccent: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val headerBackground: Color,
    val onHeaderBackground: Color,
    val onHeaderMuted: Color,
    val inactiveDot: Color,
)

val LightVitalsColors = VitalsColorPalette(
    background = Color(0xFFFBF9EC),
    surface = Color(0xFFFFFDF6),
    surfaceMuted = Color(0xFFF3F1E2),
    border = Color(0xFFE2E0CC),
    textPrimary = Color(0xFF1A4A35),
    textMuted = Color(0xFF7A8A82),
    primary = Color(0xFF1A4A35),
    onPrimary = Color(0xFFFBF9EC),
    accent = Color(0xFF7DD4A8),
    onAccent = Color(0xFF1A4A35),
    success = Color(0xFF1D6B42),
    warning = Color(0xFFB45309),
    danger = Color(0xFFC0453A),
    headerBackground = Color(0xFF1A4A35),
    onHeaderBackground = Color(0xFFF7FBF8),
    onHeaderMuted = Color(0xFFD2E6DB),
    inactiveDot = Color(0xFFC8C8C0),
)

val DarkVitalsColors = VitalsColorPalette(
    background = Color(0xFF121413),
    surface = Color(0xFF1A1D1C),
    surfaceMuted = Color(0xFF222524),
    border = Color(0xFF2A2D2C),
    textPrimary = Color(0xFFE0E0E0),
    textMuted = Color(0xFFA0A0A0),
    primary = Color(0xFF76D1A1),
    onPrimary = Color(0xFF1A1D1C),
    accent = Color(0xFF76D1A1),
    onAccent = Color(0xFF1A1D1C),
    success = Color(0xFF3FA96B),
    warning = Color(0xFFD08A3E),
    danger = Color(0xFFE0564A),
    headerBackground = Color(0xFF121413),
    onHeaderBackground = Color(0xFFE0E0E0),
    onHeaderMuted = Color(0xFFA0A0A0),
    inactiveDot = Color(0xFF3A3D3C),
)
