package com.vitals.mobile.feature.labs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.vitals.mobile.core.navigation.NavRoutes

@Composable
fun LabsScreen(
    navController: NavHostController,
    viewModel: LabsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsBackTopBar(title = "Анализы и рецепты", onBack = { navController.popBackStack() })
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(48.dp))
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
                Text(text = it, style = VitalsTheme.typography.bodySmall, color = colors.danger)
                Spacer(modifier = Modifier.height(12.dp))
            }
            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Text(text = "Лаборатория", style = VitalsTheme.typography.titleMedium, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.labLines.isEmpty()) {
                        Text(
                            text = "Пока нет назначенных или рекомендованных анализов",
                            style = VitalsTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                    } else {
                        state.labLines.forEach { line ->
                            Text(
                                text = if (line.status.isBlank()) line.title else "${line.title} - ${line.status}",
                                style = VitalsTheme.typography.bodySmall,
                                color = colors.textMuted,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Text(text = "Рецепты", style = VitalsTheme.typography.titleMedium, color = colors.textPrimary)
                    Text(
                        text = "Нажмите на рецепт, чтобы открыть детали и QR",
                        style = VitalsTheme.typography.bodySmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    )
                    if (state.prescriptionLines.isEmpty()) {
                        Text(
                            text = "Рецепты появятся после консультации",
                            style = VitalsTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                    } else {
                        state.prescriptionLines.forEach { line ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = line.id.isNotBlank()) {
                                        navController.navigate(NavRoutes.prescriptionDetail(line.id))
                                    }
                                    .padding(vertical = 8.dp),
                            ) {
                                Text(
                                    text = if (line.status.isBlank()) line.title else "${line.title} - ${line.status}",
                                    style = VitalsTheme.typography.bodyMedium,
                                    color = colors.textPrimary,
                                )
                                if (line.subtitle.isNotBlank()) {
                                    Text(
                                        text = line.subtitle,
                                        style = VitalsTheme.typography.bodySmall,
                                        color = colors.textMuted,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                                if (line.qrAvailable) {
                                    Text(
                                        text = "Есть QR для аптеки · открыть",
                                        style = VitalsTheme.typography.labelMedium,
                                        color = colors.primary,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
