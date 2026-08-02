package com.vitals.mobile.feature.consultations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
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
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
            Text(
                text = "Записи на приём и чаты с врачами",
                style = VitalsTheme.typography.bodySmall,
                color = colors.textMuted,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Запланированные", style = VitalsTheme.typography.titleSmall, color = colors.textPrimary)
            Text(
                text = "Записи на слот расписания",
                style = VitalsTheme.typography.bodySmall,
                color = colors.textMuted,
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (state.scheduled.isEmpty()) {
                Text(text = "Нет запланированных приёмов", style = VitalsTheme.typography.bodySmall, color = colors.textMuted)
            } else {
                state.scheduled.forEach { consultation ->
                    ConsultationCard(consultation) {
                        navController.navigate(NavRoutes.consultationDetail(consultation.resolvedId))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Чаты", style = VitalsTheme.typography.titleSmall, color = colors.textPrimary)
            Text(
                text = "Свободные консультации без брони слота",
                style = VitalsTheme.typography.bodySmall,
                color = colors.textMuted,
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (state.chats.isEmpty()) {
                Text(text = "Нет активных чатов", style = VitalsTheme.typography.bodySmall, color = colors.textMuted)
            } else {
                state.chats.forEach { consultation ->
                    ConsultationCard(consultation) {
                        navController.navigate(NavRoutes.consultationDetail(consultation.resolvedId))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ConsultationCard(consultation: ConsultationDto, onClick: () -> Unit) {
    val colors = VitalsTheme.colors
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
                    text = "${typeLabel(consultation.consultationType)} · ${consultation.status ?: ""}",
                    style = VitalsTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = consultation.scheduledAt?.let { "Приём: $it" } ?: "Активность: ${consultation.createdAt ?: "недавно"}",
                    style = VitalsTheme.typography.labelMedium,
                    color = colors.textPrimary,
                )
            }
            VitalsStatusChip(text = typeLabel(consultation.consultationType), tone = StatusTone.NEUTRAL)
        }
    }
}
