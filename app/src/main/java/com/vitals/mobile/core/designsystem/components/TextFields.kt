package com.vitals.mobile.core.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.util.PhoneNumber

@Composable
fun VitalsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isPassword: Boolean = false,
    isPhone: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val colors = VitalsTheme.colors
    var passwordVisible by remember { mutableStateOf(false) }

    val transformation = when {
        isPassword && !passwordVisible -> PasswordVisualTransformation()
        isPhone -> PhoneNumber.visualTransformation
        else -> VisualTransformation.None
    }

    val resolvedKeyboard = when {
        isPassword -> KeyboardType.Password
        isPhone -> KeyboardType.Phone
        else -> keyboardType
    }

    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            if (isPhone) onValueChange(PhoneNumber.normalizeDigits(raw))
            else onValueChange(raw)
        },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        minLines = minLines,
        isError = isError,
        label = label?.let { { Text(it, style = VitalsTheme.typography.bodySmall) } },
        placeholder = placeholder?.let { { Text(it, style = VitalsTheme.typography.bodyMedium) } },
        visualTransformation = transformation,
        keyboardOptions = KeyboardOptions(keyboardType = resolvedKeyboard),
        shape = VitalsTheme.shapes.input,
        supportingText = supportingText?.let { { Text(it) } },
        trailingIcon = when {
            isPassword -> {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль",
                            tint = colors.textMuted,
                        )
                    }
                }
            }
            trailingContent != null -> trailingContent
            else -> null
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceMuted.copy(alpha = 0.65f),
            unfocusedContainerColor = colors.surfaceMuted.copy(alpha = 0.45f),
            disabledContainerColor = colors.surfaceMuted.copy(alpha = 0.3f),
            focusedBorderColor = colors.accent.copy(alpha = 0.85f),
            unfocusedBorderColor = colors.border.copy(alpha = 0.65f),
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedLabelColor = colors.textMuted,
            unfocusedLabelColor = colors.textMuted.copy(alpha = 0.9f),
            cursorColor = colors.primary,
            focusedPlaceholderColor = colors.textMuted.copy(alpha = 0.7f),
            unfocusedPlaceholderColor = colors.textMuted.copy(alpha = 0.55f),
            errorBorderColor = colors.danger.copy(alpha = 0.8f),
        ),
    )
}
