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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vitals.mobile.core.data.common.ConsultationLabels
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.StatusTone
import com.vitals.mobile.core.designsystem.components.VitalsBackTopBar
import com.vitals.mobile.core.designsystem.components.VitalsCard
import com.vitals.mobile.core.designsystem.components.VitalsStatusChip
import com.vitals.mobile.core.navigation.NavRoutes
import com.vitals.mobile.feature.common.ChatBody

@Composable
fun ConsultationDetailScreen(
    sessionId: String,
    navController: NavHostController,
    viewModel: ConsultationDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors
    val consultation = state.consultation
    val protocol = consultation?.protocol
    val terminal = ConsultationLabels.isTerminal(consultation?.status)

    LaunchedEffect(sessionId) { viewModel.load(sessionId) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsBackTopBar(
            title = consultation?.doctorName ?: "Консультация",
            onBack = { navController.popBackStack() },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = "${ConsultationLabels.type(consultation?.resolvedType)} · ${ConsultationLabels.status(consultation?.status)}",
                style = VitalsTheme.typography.bodySmall,
                color = colors.textMuted,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row {
                VitalsStatusChip(
                    text = "Запись на приём",
                    tone = StatusTone.NEUTRAL,
                    modifier = Modifier.clickable {
                        val doctorId = consultation?.doctorId
                        if (!doctorId.isNullOrBlank()) {
                            navController.navigate(NavRoutes.doctorBook(doctorId))
                        } else {
                            navController.navigate(NavRoutes.DOCTORS)
                        }
                    },
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (protocol != null) {
                    VitalsStatusChip(text = "Протокол доступен", tone = StatusTone.ACCENT)
                }
            }

            if (protocol != null) {
                Spacer(modifier = Modifier.height(12.dp))
                VitalsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(15.dp)) {
                        Text(
                            text = "Протокол консультации",
                            style = VitalsTheme.typography.titleSmall,
                            color = colors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        ProtocolField(label = "Жалобы", value = protocol.complaints)
                        ProtocolField(
                            label = "Диагноз",
                            value = listOfNotNull(
                                protocol.preliminaryDiagnosisIcd10,
                                protocol.preliminaryDiagnosisText,
                            ).joinToString(" — "),
                        )
                        ProtocolField(label = "Рекомендации", value = protocol.recommendations)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            if (terminal) {
                Text(
                    text = "Консультация завершена — отправка сообщений недоступна",
                    style = VitalsTheme.typography.bodySmall,
                    color = colors.textMuted,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            ChatBody(
                messages = state.messages,
                inputText = if (terminal) "" else state.inputText,
                onInputChange = if (terminal) ({}) else viewModel::updateInput,
                onSend = { if (!terminal) viewModel.sendCurrentInput(sessionId) },
                sendEnabled = !terminal && !state.isSending,
                emptyPlaceholder = if (terminal) null else "Напишите сообщение врачу",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProtocolField(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    val colors = VitalsTheme.colors
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Text(text = label, style = VitalsTheme.typography.labelMedium, color = colors.textMuted)
        Text(text = value, style = VitalsTheme.typography.bodyMedium, color = colors.textPrimary)
    }
}
