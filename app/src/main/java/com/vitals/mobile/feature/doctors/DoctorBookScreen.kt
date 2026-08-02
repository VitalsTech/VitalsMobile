package com.vitals.mobile.feature.doctors

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vitals.mobile.core.data.common.ScheduleSlotLabels
import com.vitals.mobile.core.data.consultations.ConsultationType
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsBackTopBar
import com.vitals.mobile.core.designsystem.components.VitalsPrimaryButton
import com.vitals.mobile.core.designsystem.components.VitalsSelectableChip
import com.vitals.mobile.core.navigation.NavRoutes

@Composable
fun DoctorBookScreen(
    doctorId: String,
    navController: NavHostController,
    viewModel: DoctorBookViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors

    LaunchedEffect(doctorId) { viewModel.load(doctorId) }
    LaunchedEffect(state.bookedSessionId) {
        state.bookedSessionId?.let { sessionId ->
            navController.navigate(NavRoutes.consultationDetail(sessionId)) {
                popUpTo(NavRoutes.doctorDetail(doctorId)) { inclusive = true }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsBackTopBar(title = "Запись на приём", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(text = "Дата", style = VitalsTheme.typography.bodySmall, color = colors.textMuted)
            Spacer(modifier = Modifier.height(8.dp))
            if (state.availableDates.isEmpty() && !state.isLoading) {
                Text(
                    text = "Нет доступных дат",
                    style = VitalsTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    state.availableDates.forEach { date ->
                        VitalsSelectableChip(
                            text = ScheduleSlotLabels.formatDayLabel(date),
                            selected = date == state.selectedDate,
                            onClick = { viewModel.selectDate(date) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Время", style = VitalsTheme.typography.bodySmall, color = colors.textMuted)
            Spacer(modifier = Modifier.height(8.dp))
            if (state.slotsForDate.isEmpty() && !state.isLoading) {
                Text(
                    text = "Нет доступных слотов на эту дату",
                    style = VitalsTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    state.slotsForDate.forEach { slot ->
                        VitalsSelectableChip(
                            text = ScheduleSlotLabels.formatTime(slot),
                            selected = slot.resolvedId == state.selectedSlotId,
                            onClick = { viewModel.selectSlot(slot.resolvedId) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Тип приёма", style = VitalsTheme.typography.bodySmall, color = colors.textMuted)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val types = listOf(
                    ConsultationType.IN_PERSON to "Очно",
                    ConsultationType.VIDEO to "Видео",
                    ConsultationType.SYNC_CHAT to "Чат",
                )
                types.forEach { (type, label) ->
                    VitalsSelectableChip(
                        text = label,
                        selected = type == state.consultationType,
                        onClick = { viewModel.selectType(type) },
                    )
                }
            }

            state.errorMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = it, style = VitalsTheme.typography.bodySmall, color = colors.danger)
            }

            Spacer(modifier = Modifier.height(28.dp))
            VitalsPrimaryButton(
                text = if (state.isSubmitting) "Записываем..." else "Подтвердить",
                onClick = { viewModel.confirm(doctorId) },
                enabled = !state.isSubmitting && state.selectedSlotId != null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
