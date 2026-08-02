package com.vitals.mobile.core.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/** Phone helpers matching VitalsWeb: store digits only (7900…), format for display. */
object PhoneNumber {
    fun normalizeDigits(raw: String): String {
        var digits = raw.filter { it.isDigit() }
        if (digits.startsWith("8")) {
            digits = "7" + digits.drop(1)
        }
        if (digits.isNotEmpty() && !digits.startsWith("7")) {
            digits = "7$digits"
        }
        return digits.take(11)
    }

    fun formatDisplay(digits: String): String {
        if (digits.isEmpty()) return ""
        val limited = digits.take(11)
        return when {
            limited.length == 1 -> "+$limited"
            limited.length <= 4 -> "+${limited.take(1)} ${limited.drop(1)}"
            limited.length <= 7 -> "+${limited.take(1)} ${limited.substring(1, 4)} ${limited.drop(4)}"
            limited.length <= 9 ->
                "+${limited.take(1)} ${limited.substring(1, 4)} ${limited.substring(4, 7)}-${limited.drop(7)}"
            else ->
                "+${limited.take(1)} ${limited.substring(1, 4)} ${limited.substring(4, 7)}-" +
                    "${limited.substring(7, 9)}-${limited.drop(9)}"
        }
    }

    fun isValidForApi(digits: String): Boolean = digits.length in 10..15

    /** Formats digit-only value as +7 … without breaking TextField cursor. */
    val visualTransformation: VisualTransformation = VisualTransformation { text ->
        val digits = text.text
        val formatted = formatDisplay(digits)
        TransformedText(AnnotatedString(formatted), PhoneOffsetMapping(digits, formatted))
    }
}

/**
 * Maps cursor between raw digits (e.g. 79001234567) and formatted (+7 900 123-45-67).
 */
private class PhoneOffsetMapping(
    private val digits: String,
    private val formatted: String,
) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        if (digits.isEmpty()) return 0
        val clamped = offset.coerceIn(0, digits.length)
        if (clamped == 0) return 0
        var seen = 0
        for (i in formatted.indices) {
            if (formatted[i].isDigit()) {
                seen++
                if (seen == clamped) return i + 1
            }
        }
        return formatted.length
    }

    override fun transformedToOriginal(offset: Int): Int {
        if (digits.isEmpty()) return 0
        val clamped = offset.coerceIn(0, formatted.length)
        var seen = 0
        for (i in 0 until clamped) {
            if (formatted[i].isDigit()) seen++
        }
        return seen.coerceIn(0, digits.length)
    }
}
