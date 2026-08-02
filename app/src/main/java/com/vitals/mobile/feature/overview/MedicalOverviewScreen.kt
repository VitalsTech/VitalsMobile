package com.vitals.mobile.feature.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import com.vitals.mobile.core.designsystem.components.VitalsSecondaryButton

@Composable
fun MedicalOverviewScreen(
    navController: NavHostController,
    viewModel: MedicalOverviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsBackTopBar(title = "Сводка и диагнозы", onBack = { navController.popBackStack() })
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    Text(
                        text = "Анамнез",
                        style = VitalsTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.summary,
                        style = VitalsTheme.typography.bodyMedium,
                        color = colors.textMuted,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Аллергии: ${state.allergies}",
                        style = VitalsTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Группа крови: ${state.bloodType}",
                        style = VitalsTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            BulletCard(title = "Активные диагнозы", items = state.diagnoses.map { it.label }, empty = "Диагнозы появятся после консультации врача")
            Spacer(modifier = Modifier.height(12.dp))
            BulletCard(title = "Препараты", items = state.medications, empty = "Препараты пока не указаны")
            Spacer(modifier = Modifier.height(12.dp))
            BulletCard(title = "Последние анализы", items = state.recentLabs, empty = "Анализы пока не указаны")

            Spacer(modifier = Modifier.height(16.dp))
            VitalsSecondaryButton(
                text = "Обновить",
                onClick = viewModel::reload,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BulletCard(title: String, items: List<String>, empty: String) {
    val colors = VitalsTheme.colors
    VitalsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(15.dp)) {
            Text(text = title, style = VitalsTheme.typography.titleMedium, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            if (items.isEmpty()) {
                Text(text = empty, style = VitalsTheme.typography.bodySmall, color = colors.textMuted)
            } else {
                items.forEach { item ->
                    Text(
                        text = "· $item",
                        style = VitalsTheme.typography.bodySmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
        }
    }
}
