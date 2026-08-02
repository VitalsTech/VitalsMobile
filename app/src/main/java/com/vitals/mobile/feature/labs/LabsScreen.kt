package com.vitals.mobile.feature.labs

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
fun LabsScreen(
    navController: NavHostController,
    viewModel: LabsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsBackTopBar(title = "Анализы", onBack = { navController.popBackStack() })
        Column(modifier = Modifier.padding(20.dp)) {
            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Text(text = "Лаборатория", style = VitalsTheme.typography.titleMedium, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.labOrders.isEmpty()) {
                        Text(text = "Ожидают назначения", style = VitalsTheme.typography.bodySmall, color = colors.textMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Анализ крови — не назначен", style = VitalsTheme.typography.bodySmall, color = colors.textMuted)
                    } else {
                        state.labOrders.forEach { order ->
                            val names = order.tests?.joinToString(", ") { it.name } ?: "Анализ"
                            Text(
                                text = "$names — ${order.status ?: "в ожидании"}",
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
                    Text(text = "Рецепт", style = VitalsTheme.typography.titleMedium, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.prescriptions.isEmpty()) {
                        Text(text = "После консультации", style = VitalsTheme.typography.bodySmall, color = colors.textMuted)
                    } else {
                        state.prescriptions.forEach { prescription ->
                            val meds = prescription.medications?.joinToString(", ") { it.name } ?: "Рецепт"
                            Text(
                                text = "$meds — ${prescription.status ?: ""}",
                                style = VitalsTheme.typography.bodySmall,
                                color = colors.textMuted,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
