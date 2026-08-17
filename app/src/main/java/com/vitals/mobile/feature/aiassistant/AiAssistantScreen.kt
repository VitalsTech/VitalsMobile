package com.vitals.mobile.feature.aiassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsMainTopBar
import com.vitals.mobile.core.designsystem.components.VitalsPrimaryButton
import com.vitals.mobile.core.designsystem.components.VitalsSecondaryButton
import com.vitals.mobile.core.navigation.NavRoutes
import com.vitals.mobile.feature.common.ChatBody

@Composable
fun AiAssistantScreen(
    navController: NavHostController,
    viewModel: AiAssistantViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.onScreenVisible()
        }
    }

    LaunchedEffect(state.completedSessionId) {
        val id = state.completedSessionId ?: return@LaunchedEffect
        viewModel.consumeCompletedNavigation()
        navController.navigate(NavRoutes.triageResult(id))
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsMainTopBar()

        if (state.isLoading || state.isStartingNew) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
            return
        }

        if (state.errorMessage != null && state.sessionId == null) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = state.errorMessage.orEmpty(),
                    style = VitalsTheme.typography.bodyMedium,
                    color = colors.danger,
                )
                Spacer(modifier = Modifier.height(16.dp))
                VitalsPrimaryButton(
                    text = "Повторить",
                    onClick = viewModel::retry,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            return
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "ИИ-триаж",
                    style = VitalsTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                if (state.sessionId != null) {
                    TextButton(
                        onClick = viewModel::startNewTriage,
                        enabled = !state.isSending && !state.isCompleting,
                    ) {
                        Text(
                            text = "Новый триаж",
                            style = VitalsTheme.typography.labelLarge,
                            color = colors.primary,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            state.errorMessage?.let {
                Text(text = it, style = VitalsTheme.typography.bodySmall, color = colors.danger)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (state.isSessionCompleted && !imeVisible) {
                Text(
                    text = "Триаж завершён. Можно открыть результат или начать новый.",
                    style = VitalsTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                state.completeSuggestion?.takeIf { state.readyToComplete && !imeVisible }?.let { suggestion ->
                    Text(
                        text = suggestion,
                        style = VitalsTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            ChatBody(
                messages = state.messages,
                inputText = state.inputText,
                onInputChange = viewModel::updateInput,
                onSend = viewModel::sendCurrentInput,
                quickReplies = if (imeVisible || state.isSessionCompleted) {
                    emptyList()
                } else {
                    listOf("Стало хуже", "Консультация", "Контроль АД")
                },
                onQuickReply = { text ->
                    viewModel.updateInput(text)
                    viewModel.sendCurrentInput()
                },
                sendEnabled = !state.isSending && !state.isCompleting && !state.isSessionCompleted,
                isThinking = state.isSending || state.isCompleting,
                thinkingLabel = if (state.isCompleting) "Завершаем триаж…" else "ИИ печатает…",
                emptyPlaceholder = "Опишите жалобу - ИИ задаст уточняющие вопросы",
                inputPlaceholder = if (state.isSessionCompleted) "Триаж завершён" else "Сообщение...",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            if (!imeVisible) {
                Spacer(modifier = Modifier.height(12.dp))
                when {
                    state.isSessionCompleted -> {
                        VitalsPrimaryButton(
                            text = "Результат триажа",
                            onClick = {
                                state.sessionId?.let { navController.navigate(NavRoutes.triageResult(it)) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        VitalsSecondaryButton(
                            text = "Начать новый триаж",
                            onClick = viewModel::startNewTriage,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    state.readyToComplete -> {
                        VitalsPrimaryButton(
                            text = if (state.isCompleting) "Завершаем…" else "Завершить триаж",
                            onClick = viewModel::completeTriage,
                            enabled = !state.isCompleting && !state.isSending,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    else -> {
                        VitalsSecondaryButton(
                            text = if (state.isCompleting) "Завершаем…" else "Завершить триаж",
                            onClick = viewModel::completeTriage,
                            enabled = !state.isCompleting && !state.isSending && state.messages.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
