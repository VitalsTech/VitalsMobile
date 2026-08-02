package com.vitals.mobile.feature.triage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

    LaunchedEffect(state.completedSessionId) {
        state.completedSessionId?.let { sessionId ->
            navController.navigate(NavRoutes.triageResult(sessionId))
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Text(
            text = "ИИ-триаж",
            style = VitalsTheme.typography.headlineMedium,
            color = colors.textPrimary,
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp),
        )
        Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
            ChatBody(
                messages = state.messages,
                inputText = state.inputText,
                onInputChange = viewModel::updateInput,
                onSend = viewModel::sendCurrentInput,
                quickReplies = listOf("Стало хуже", "Консультация", "Контроль АД"),
                onQuickReply = viewModel::sendQuickReply,
                sendEnabled = !state.isSending,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            TextButton(onClick = viewModel::completeTriage, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Завершить триаж и посмотреть результат", color = colors.textMuted)
            }
        }
    }
}
