package com.vitals.mobile.feature.triage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsBackTopBar
import com.vitals.mobile.core.designsystem.components.VitalsCard
import com.vitals.mobile.core.designsystem.components.VitalsPrimaryButton
import com.vitals.mobile.core.designsystem.components.VitalsSecondaryButton
import com.vitals.mobile.core.navigation.NavRoutes

@Composable
fun TriageResultScreen(
    sessionId: String,
    navController: NavHostController,
    viewModel: TriageResultViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors

    LaunchedEffect(sessionId) { viewModel.load(sessionId) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsBackTopBar(title = "Результат триажа", onBack = { navController.popBackStack() })
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                CircularProgressIndicator(color = colors.primary)
            }
            return
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            state.errorMessage?.let {
                Text(text = it, style = VitalsTheme.typography.bodyMedium, color = colors.danger)
                Spacer(modifier = Modifier.height(12.dp))
            }

            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Text(
                        text = state.urgencyTitle.ifBlank { "Результат триажа" },
                        style = VitalsTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                    if (state.urgencySubtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.urgencySubtitle,
                            style = VitalsTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                    }
                    if (state.recommendation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.recommendation,
                            style = VitalsTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                        )
                    }
                }
            }

            if (!state.assignedDoctorName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                VitalsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(15.dp)) {
                        Text(
                            text = "Назначенный врач",
                            style = VitalsTheme.typography.titleSmall,
                            color = colors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.assignedDoctorName.orEmpty(),
                            style = VitalsTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Text(
                        text = "Рекомендованный маршрут",
                        style = VitalsTheme.typography.titleSmall,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.routeSteps.isEmpty()) {
                        Text(
                            text = "Маршрут формируется…",
                            style = VitalsTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                    } else {
                        state.routeSteps.forEachIndexed { index, step ->
                            Text(
                                text = "${index + 1}. $step",
                                style = VitalsTheme.typography.bodyMedium,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    }
                }
            }

            if (state.recommendedLabs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                VitalsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(15.dp)) {
                        Text(
                            text = "Рекомендованные анализы",
                            style = VitalsTheme.typography.titleSmall,
                            color = colors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        state.recommendedLabs.forEach { lab ->
                            Text(
                                text = "· $lab",
                                style = VitalsTheme.typography.bodySmall,
                                color = colors.textMuted,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            when {
                !state.consultationSessionId.isNullOrBlank() -> {
                    VitalsPrimaryButton(
                        text = "Открыть консультацию",
                        onClick = {
                            navController.navigate(NavRoutes.consultationDetail(state.consultationSessionId!!))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                !state.assignedDoctorId.isNullOrBlank() -> {
                    VitalsPrimaryButton(
                        text = "Записаться к врачу",
                        onClick = {
                            navController.navigate(NavRoutes.doctorBook(state.assignedDoctorId!!))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    VitalsSecondaryButton(
                        text = "Карточка врача",
                        onClick = {
                            navController.navigate(NavRoutes.doctorDetail(state.assignedDoctorId!!))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> {
                    VitalsPrimaryButton(
                        text = "Выбрать врача",
                        onClick = { navController.navigate(NavRoutes.DOCTORS) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            VitalsSecondaryButton(
                text = "Начать новый триаж",
                onClick = {
                    viewModel.startNewTriage {
                        navController.navigate(NavRoutes.TRIAGE_CHAT) {
                            popUpTo(NavRoutes.PATH) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            VitalsSecondaryButton(
                text = "На «Мой путь»",
                onClick = {
                    navController.navigate(NavRoutes.PATH) {
                        popUpTo(NavRoutes.PATH) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
