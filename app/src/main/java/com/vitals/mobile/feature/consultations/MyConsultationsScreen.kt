package com.vitals.mobile.feature.consultations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vitals.mobile.core.data.common.ConsultationLabels
import com.vitals.mobile.core.data.common.ScheduleSlotLabels
import com.vitals.mobile.core.data.consultations.ConsultationDto
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.StatusTone
import com.vitals.mobile.core.designsystem.components.VitalsBackTopBar
import com.vitals.mobile.core.designsystem.components.VitalsCard
import com.vitals.mobile.core.designsystem.components.VitalsStatusChip
import com.vitals.mobile.core.navigation.NavRoutes

@Composable
fun MyConsultationsScreen(
    navController: NavHostController,
    viewModel: MyConsultationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsBackTopBar(title = "Мои консультации", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Text(
                text = "Записи на приём и чаты с врачами",
                style = VitalsTheme.typography.bodySmall,
                color = colors.textMuted,
            )
            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle(
                title = "Запланированные",
                subtitle = "Активные записи на слот расписания",
            )
            Spacer(modifier = Modifier.height(10.dp))
            ConsultationList(
                items = state.scheduled,
                emptyText = "Нет запланированных приёмов",
                onOpen = { navController.navigate(NavRoutes.consultationDetail(it)) },
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(
                title = "Чаты",
                subtitle = "Активные консультации без брони слота",
            )
            Spacer(modifier = Modifier.height(10.dp))
            ConsultationList(
                items = state.chats,
                emptyText = "Нет активных чатов",
                onOpen = { navController.navigate(NavRoutes.consultationDetail(it)) },
            )

            if (state.completed.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                SectionTitle(
                    title = "Завершённые",
                    subtitle = "Закрытые консультации и приёмы",
                )
                Spacer(modifier = Modifier.height(10.dp))
                ConsultationList(
                    items = state.completed,
                    emptyText = "",
                    onOpen = { navController.navigate(NavRoutes.consultationDetail(it)) },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    val colors = VitalsTheme.colors
    Text(text = title, style = VitalsTheme.typography.titleSmall, color = colors.textPrimary)
    Text(text = subtitle, style = VitalsTheme.typography.bodySmall, color = colors.textMuted)
}

@Composable
private fun ConsultationList(
    items: List<ConsultationDto>,
    emptyText: String,
    onOpen: (String) -> Unit,
) {
    val colors = VitalsTheme.colors
    if (items.isEmpty()) {
        if (emptyText.isNotBlank()) {
            Text(text = emptyText, style = VitalsTheme.typography.bodySmall, color = colors.textMuted)
        }
        return
    }
    items.forEach { consultation ->
        ConsultationCard(consultation) { onOpen(consultation.resolvedId) }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ConsultationCard(consultation: ConsultationDto, onClick: () -> Unit) {
    val colors = VitalsTheme.colors
    val type = consultation.resolvedType
    VitalsCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(15.dp).fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = consultation.doctorName ?: "Врач",
                    style = VitalsTheme.typography.titleSmall,
                    color = colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${typeLabel(type)} · ${ConsultationLabels.status(consultation.status)}",
                    style = VitalsTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        consultation.isSlotBooking() && !consultation.scheduledAt.isNullOrBlank() ->
                            "Приём: ${ScheduleSlotLabels.formatDayTime(consultation.scheduledAt)}"
                        else ->
                            "Активность: ${ScheduleSlotLabels.formatDayTime(
                                consultation.lastActivityAt ?: consultation.completedAt ?: consultation.createdAt,
                            )}"
                    },
                    style = VitalsTheme.typography.labelMedium,
                    color = colors.textPrimary,
                )
            }
            VitalsStatusChip(text = typeLabel(type), tone = StatusTone.NEUTRAL)
        }
    }
}
