package com.vitals.mobile.feature.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitals.mobile.core.data.auth.EsiaMessages
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsCard
import com.vitals.mobile.core.designsystem.components.VitalsPrimaryButton
import com.vitals.mobile.core.designsystem.components.VitalsSecondaryButton
import com.vitals.mobile.core.designsystem.components.VitalsTextField
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors

    if (state.isAuthenticated) {
        onAuthenticated()
        return
    }

    when {
        state.esiaNotice != null -> {
            EsiaNoticeScreen(state = state, viewModel = viewModel)
            return
        }
        state.showEsiaForm -> {
            EsiaStubFormScreen(state = state, viewModel = viewModel)
            return
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.background,
                        colors.surfaceMuted.copy(alpha = 0.55f),
                        colors.background,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Vitals",
                    style = VitalsTheme.typography.headlineLarge,
                    color = colors.textPrimary,
                )
                Text(
                    text = "Пациент",
                    style = VitalsTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = if (state.tab == AuthTab.LOGIN) "Добро пожаловать" else "Создать аккаунт",
                    style = VitalsTheme.typography.displaySmall,
                    color = colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (state.tab == AuthTab.LOGIN) {
                        "Войдите, чтобы продолжить маршрут лечения"
                    } else {
                        "Несколько полей - и вы в портале пациента"
                    },
                    style = VitalsTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
                Spacer(modifier = Modifier.height(22.dp))

                VitalsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        AuthTabSwitcher(
                            selected = state.tab,
                            onSelect = viewModel::selectTab,
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        if (state.tab == AuthTab.LOGIN) {
                            LoginForm(state = state, viewModel = viewModel)
                        } else {
                            RegisterForm(state = state, viewModel = viewModel)
                        }

                        state.errorMessage?.let { message ->
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = message,
                                style = VitalsTheme.typography.bodySmall,
                                color = colors.danger,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AuthTabSwitcher(selected: AuthTab, onSelect: (AuthTab) -> Unit) {
    val colors = VitalsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceMuted)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AuthTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val bg by animateColorAsState(
                targetValue = if (isSelected) colors.surface else colors.surfaceMuted,
                animationSpec = tween(180),
                label = "tabBg",
            )
            val fg by animateColorAsState(
                targetValue = if (isSelected) colors.textPrimary else colors.textMuted,
                animationSpec = tween(180),
                label = "tabFg",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(bg)
                    .then(
                        if (isSelected) {
                            Modifier.border(1.dp, colors.border.copy(alpha = 0.7f), RoundedCornerShape(11.dp))
                        } else {
                            Modifier
                        },
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(tab) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (tab == AuthTab.LOGIN) "Вход" else "Регистрация",
                    style = VitalsTheme.typography.labelLarge,
                    color = fg,
                )
            }
        }
    }
}

@Composable
private fun LoginForm(state: AuthUiState, viewModel: AuthViewModel) {
    Column {
        VitalsTextField(
            value = state.loginPhoneDigits,
            onValueChange = viewModel::updateLoginPhone,
            label = "Телефон",
            placeholder = "+7 900 000-00-00",
            isPhone = true,
        )
        Spacer(modifier = Modifier.height(14.dp))
        VitalsTextField(
            value = state.loginPassword,
            onValueChange = viewModel::updateLoginPassword,
            label = "Пароль",
            placeholder = "••••••••",
            isPassword = true,
        )
        Spacer(modifier = Modifier.height(20.dp))
        VitalsPrimaryButton(
            text = "Войти",
            onClick = viewModel::submitLogin,
            modifier = Modifier.fillMaxWidth(),
            loading = state.isLoading,
        )
        if (state.esiaEnabled) {
            Spacer(modifier = Modifier.height(10.dp))
            VitalsSecondaryButton(
                text = "Войти через Госуслуги",
                onClick = viewModel::openEsiaForm,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterForm(state: AuthUiState, viewModel: AuthViewModel) {
    var showDatePicker by remember { mutableStateOf(false) }
    val displayFormat = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    Column {
        VitalsTextField(
            value = state.registerSecondName,
            onValueChange = { viewModel.updateRegisterField(secondName = it) },
            label = "Фамилия",
            placeholder = "Иванова",
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VitalsTextField(
                value = state.registerFirstName,
                onValueChange = { viewModel.updateRegisterField(firstName = it) },
                label = "Имя",
                placeholder = "Анна",
                modifier = Modifier.weight(1f),
            )
            VitalsTextField(
                value = state.registerSurename,
                onValueChange = { viewModel.updateRegisterField(surename = it) },
                label = "Отчество",
                placeholder = "Сергеевна",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.clickable { showDatePicker = true }) {
            VitalsTextField(
                value = state.registerBirthDate,
                onValueChange = {},
                label = "Дата рождения",
                placeholder = "Выберите дату",
                readOnly = true,
                trailingContent = {
                    IconButtonCalendar { showDatePicker = true }
                },
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        SexPicker(
            isFemale = state.registerSexIsFemale,
            onChange = { viewModel.updateRegisterField(sexIsFemale = it) },
        )
        Spacer(modifier = Modifier.height(12.dp))
        VitalsTextField(
            value = state.registerPhoneDigits,
            onValueChange = { viewModel.updateRegisterField(phoneRaw = it) },
            label = "Телефон",
            placeholder = "+7 900 000-00-00",
            isPhone = true,
        )
        Spacer(modifier = Modifier.height(12.dp))
        VitalsTextField(
            value = state.registerPassword,
            onValueChange = { viewModel.updateRegisterField(password = it) },
            label = "Пароль",
            placeholder = "••••••••",
            isPassword = true,
        )
        Spacer(modifier = Modifier.height(20.dp))
        VitalsPrimaryButton(
            text = "Зарегистрироваться",
            onClick = viewModel::submitRegister,
            modifier = Modifier.fillMaxWidth(),
            loading = state.isLoading,
        )
        if (state.esiaEnabled) {
            Spacer(modifier = Modifier.height(10.dp))
            VitalsSecondaryButton(
                text = "Войти через Госуслуги",
                onClick = viewModel::openEsiaForm,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            )
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(state.registerBirthDate) {
            runCatching {
                LocalDate.parse(state.registerBirthDate, displayFormat)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            viewModel.updateRegisterField(birthDate = date.format(displayFormat))
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("Выбрать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun IconButtonCalendar(onClick: () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.CalendarMonth,
            contentDescription = "Календарь",
            tint = VitalsTheme.colors.textMuted,
        )
    }
}

@Composable
private fun SexPicker(isFemale: Boolean, onChange: (Boolean) -> Unit) {
    val colors = VitalsTheme.colors
    Column {
        Text(
            text = "Пол",
            style = VitalsTheme.typography.labelMedium,
            color = colors.textMuted,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SexChip(
                label = "Женский",
                selected = isFemale,
                onClick = { onChange(true) },
                modifier = Modifier.weight(1f),
            )
            SexChip(
                label = "Мужской",
                selected = !isFemale,
                onClick = { onChange(false) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SexChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VitalsTheme.colors
    val bg by animateColorAsState(
        targetValue = if (selected) colors.accent.copy(alpha = 0.22f) else colors.surfaceMuted,
        animationSpec = tween(160),
        label = "sexBg",
    )
    val border by animateColorAsState(
        targetValue = if (selected) colors.accent.copy(alpha = 0.55f) else colors.border,
        animationSpec = tween(160),
        label = "sexBorder",
    )
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = VitalsTheme.typography.labelLarge,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun EsiaStubFormScreen(state: AuthUiState, viewModel: AuthViewModel) {
    val colors = VitalsTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.background,
                        colors.surfaceMuted.copy(alpha = 0.55f),
                        colors.background,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
        ) {
            Text(
                text = "Войти через Госуслуги",
                style = VitalsTheme.typography.displaySmall,
                color = colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(22.dp))
            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    VitalsTextField(
                        value = state.esiaLastName,
                        onValueChange = { viewModel.updateEsiaField(lastName = it) },
                        label = "Фамилия",
                        placeholder = "Иванов",
                        isError = state.esiaLastNameError != null,
                        supportingText = state.esiaLastNameError,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    VitalsTextField(
                        value = state.esiaFirstName,
                        onValueChange = { viewModel.updateEsiaField(firstName = it) },
                        label = "Имя",
                        placeholder = "Иван",
                        isError = state.esiaFirstNameError != null,
                        supportingText = state.esiaFirstNameError,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    VitalsTextField(
                        value = state.esiaMiddleName,
                        onValueChange = { viewModel.updateEsiaField(middleName = it) },
                        label = "Отчество",
                        placeholder = "Иванович",
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    VitalsTextField(
                        value = state.esiaEmail,
                        onValueChange = { viewModel.updateEsiaField(email = it) },
                        label = "Почта",
                        placeholder = "ivan@example.com",
                        keyboardType = KeyboardType.Email,
                        isError = state.esiaEmailError != null,
                        supportingText = state.esiaEmailError,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    VitalsTextField(
                        value = state.esiaPhoneDigits,
                        onValueChange = { viewModel.updateEsiaField(phoneRaw = it) },
                        label = "Телефон",
                        placeholder = "+7 900 000-00-00",
                        isPhone = true,
                        isError = state.esiaPhoneError != null,
                        supportingText = state.esiaPhoneError,
                    )
                    state.esiaError?.let { message ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = message,
                            style = VitalsTheme.typography.bodySmall,
                            color = colors.danger,
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    VitalsPrimaryButton(
                        text = "Продолжить",
                        onClick = viewModel::submitEsia,
                        modifier = Modifier.fillMaxWidth(),
                        loading = state.esiaSubmitting,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    VitalsSecondaryButton(
                        text = "Назад",
                        onClick = viewModel::closeEsiaForm,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.esiaSubmitting,
                    )
                }
            }
        }
    }
}

@Composable
private fun EsiaNoticeScreen(state: AuthUiState, viewModel: AuthViewModel) {
    val colors = VitalsTheme.colors
    val notice = state.esiaNotice ?: return
    val clipboard = LocalClipboardManager.current
    val password = notice.devPassword

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        VitalsCard(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Госуслуги",
                    style = VitalsTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (notice.existingAccount) {
                        EsiaMessages.EXISTING_ACCOUNT
                    } else {
                        "Аккаунт создан через Госуслуги."
                    },
                    style = VitalsTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                )
                if (!notice.existingAccount && !password.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Пароль для входа по телефону",
                        style = VitalsTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = password,
                        style = VitalsTheme.typography.titleSmall,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    VitalsSecondaryButton(
                        text = if (state.esiaPasswordCopied) "Скопировано" else "Скопировать",
                        onClick = {
                            clipboard.setText(AnnotatedString(password))
                            viewModel.markEsiaPasswordCopied()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                state.esiaError?.let { message ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = message,
                        style = VitalsTheme.typography.bodySmall,
                        color = colors.danger,
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                VitalsPrimaryButton(
                    text = "Продолжить",
                    onClick = viewModel::confirmEsiaNotice,
                    modifier = Modifier.fillMaxWidth(),
                    loading = state.esiaSubmitting,
                )
            }
        }
    }
}
