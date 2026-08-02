package com.vitals.mobile.feature.treatment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsBackTopBar
import com.vitals.mobile.core.designsystem.components.VitalsCard

@Composable
fun TreatmentScreen(
    navController: NavHostController,
    viewModel: TreatmentViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsBackTopBar(title = "Лечение", onBack = { navController.popBackStack() })
        Column(modifier = Modifier.padding(20.dp)) {
            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Text(text = "Выполнено", style = VitalsTheme.typography.titleSmall, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(10.dp))
                    if (state.done.isEmpty()) {
                        Text(text = "Пока ничего не выполнено", style = VitalsTheme.typography.bodySmall, color = colors.textMuted)
                    } else {
                        Text(
                            text = state.done.joinToString(" · "),
                            style = VitalsTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Text(text = "Ожидает", style = VitalsTheme.typography.titleSmall, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(10.dp))
                    if (state.pending.isEmpty()) {
                        Text(
                            text = "Нет ожидающих шагов",
                            style = VitalsTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                    } else {
                        state.pending.forEach { item ->
                            Text(
                                text = "• $item",
                                style = VitalsTheme.typography.bodyMedium,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
