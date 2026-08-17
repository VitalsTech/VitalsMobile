package com.vitals.mobile.feature.triage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.navigation.NavRoutes
import com.vitals.mobile.feature.common.ChatBody

@Composable
fun TriageOnboardingScreen(
    navController: NavHostController,
    viewModel: TriageViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(state.completedSessionId) {
        state.completedSessionId?.let { sessionId ->
            navController.navigate(NavRoutes.triageResult(sessionId))
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 12.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "ИИ-триаж",
                style = VitalsTheme.typography.headlineMedium,
                color = colors.textPrimary,
            )
            TextButton(
                onClick = viewModel::startNewTriage,
                enabled = !state.isSending,
            ) {
                Text(text = "Новый триаж", color = colors.primary)
            }
        }
        Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
            state.errorMessage?.let {
                Text(text = it, style = VitalsTheme.typography.bodySmall, color = colors.danger)
            }
            ChatBody(
                messages = state.messages,
                inputText = state.inputText,
                onInputChange = viewModel::updateInput,
                onSend = viewModel::sendCurrentInput,
                quickReplies = if (imeVisible) emptyList() else listOf("Стало хуже", "Консультация", "Контроль АД"),
                onQuickReply = viewModel::sendQuickReply,
                sendEnabled = !state.isSending,
                isThinking = state.isSending,
                thinkingLabel = "ИИ печатает…",
                emptyPlaceholder = "Опишите жалобу - ИИ задаст уточняющие вопросы",
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            if (!imeVisible) {
                TextButton(onClick = viewModel::completeTriage, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Завершить триаж и посмотреть результат", color = colors.textMuted)
                }
            }
        }
    }
}
