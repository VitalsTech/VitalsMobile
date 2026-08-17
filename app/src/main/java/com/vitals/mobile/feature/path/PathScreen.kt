package com.vitals.mobile.feature.path

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsCard
import com.vitals.mobile.core.designsystem.components.VitalsMainTopBar
import com.vitals.mobile.core.designsystem.components.VitalsPrimaryButton
import com.vitals.mobile.core.designsystem.components.VitalsSecondaryButton
import com.vitals.mobile.core.designsystem.components.VitalsSelectableChip
import com.vitals.mobile.core.data.medicalrecords.MoodCode
import com.vitals.mobile.core.navigation.NavRoutes

@Composable
fun PathScreen(
    navController: NavHostController,
    viewModel: PathViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.infoMessage) {
        state.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeInfoMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            VitalsMainTopBar()
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                ) {
                    Text(
                        text = "Здравствуйте, ${state.greetingName}",
                        style = VitalsTheme.typography.displaySmall,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (state.totalSteps == 0) {
                            "Активный маршрут пока не назначен"
                        } else {
                            "Шаг ${state.currentStepIndex} из ${state.totalSteps} · маршрут наблюдения"
                        },
                        style = VitalsTheme.typography.bodyMedium,
                        color = colors.textMuted,
                    )
                    state.errorMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = it, style = VitalsTheme.typography.bodySmall, color = colors.danger)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    VitalsCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(15.dp)) {
                            Text(
                                text = "Ваш маршрут",
                                style = VitalsTheme.typography.titleMedium,
                                color = colors.textPrimary,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (state.steps.isEmpty()) {
                                Text(
                                    text = "Пройдите ИИ-триаж - мы соберём персональный маршрут",
                                    style = VitalsTheme.typography.bodySmall,
                                    color = colors.textMuted,
                                )
                            } else {
                                state.steps.forEach { step ->
                                    RouteStepRow(step)
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    VitalsPrimaryButton(
                        text = state.continueLabel,
                        onClick = { navController.navigate(viewModel.continueRoute()) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    VitalsSecondaryButton(
                        text = "Начать новый ИИ-триаж",
                        onClick = {
                            viewModel.startNewTriage {
                                navController.navigate(NavRoutes.TRIAGE_CHAT) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    VitalsSecondaryButton(
                        text = "Сводка и диагнозы",
                        onClick = { navController.navigate(NavRoutes.MEDICAL_OVERVIEW) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VitalsSecondaryButton(
                            text = "Вызов на дом",
                            onClick = { navController.navigate(NavRoutes.HOUSE_CALL) },
                            modifier = Modifier.weight(1f),
                        )
                        VitalsSecondaryButton(
                            text = "Лечение",
                            onClick = { navController.navigate(NavRoutes.TREATMENT) },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Как себя чувствуете?",
                        style = VitalsTheme.typography.titleSmall,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Краткая отметка обновит маршрут и уведомит врача",
                        style = VitalsTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    MoodCode.entries.forEach { mood ->
                        VitalsSelectableChip(
                            text = mood.label,
                            selected = state.selectedMood == mood,
                            onClick = {
                                if (!state.isSavingMood) viewModel.reportMood(mood)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                        )
                    }
                    if (state.selectedMood == MoodCode.WORSE) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Если стало хуже - пройдите короткий ИИ-триаж, чтобы обновить маршрут.",
                            style = VitalsTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        VitalsSecondaryButton(
                            text = "К ИИ-триажу",
                            onClick = { navController.navigate(NavRoutes.TRIAGE_CHAT) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isSavingMood,
                        )
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun RouteStepRow(step: RouteStepUi) {
    val colors = VitalsTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(if (step.isCurrent) colors.accent else colors.inactiveDot),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${step.index}. ${step.title}",
                    style = VitalsTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (step.status.isNotBlank()) {
                    Text(
                        text = step.status,
                        style = VitalsTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                }
            }
            if (step.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = step.description,
                    style = VitalsTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
            }
        }
    }
}
