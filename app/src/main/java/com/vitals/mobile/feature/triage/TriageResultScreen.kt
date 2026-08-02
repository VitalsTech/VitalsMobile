package com.vitals.mobile.feature.triage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsBackTopBar
import com.vitals.mobile.core.designsystem.components.VitalsCard
import com.vitals.mobile.core.designsystem.components.VitalsPrimaryButton
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
        VitalsBackTopBar(title = "Результат", onBack = { navController.popBackStack() })
        Column(modifier = Modifier.padding(20.dp)) {
            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Text(text = state.urgencyTitle, style = VitalsTheme.typography.titleMedium, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = state.urgencySubtitle, style = VitalsTheme.typography.bodySmall, color = colors.textMuted)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Text(text = "Шаги маршрута", style = VitalsTheme.typography.titleSmall, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
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
            Spacer(modifier = Modifier.height(20.dp))
            VitalsPrimaryButton(
                text = "Начать маршрут",
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
