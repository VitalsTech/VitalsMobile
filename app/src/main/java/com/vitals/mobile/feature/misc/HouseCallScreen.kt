package com.vitals.mobile.feature.misc

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
import com.vitals.mobile.core.designsystem.components.VitalsPrimaryButton
import com.vitals.mobile.core.designsystem.components.VitalsTextField

@Composable
fun HouseCallScreen(
    navController: NavHostController,
    viewModel: HouseCallViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors

    LaunchedEffect(state.submitted) {
        if (state.submitted) navController.popBackStack()
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsBackTopBar(title = "Вызов на дом", onBack = { navController.popBackStack() })
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Симптомы и адрес для подтверждения",
                style = VitalsTheme.typography.bodySmall,
                color = colors.textMuted,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Адрес", style = VitalsTheme.typography.labelMedium, color = colors.textMuted)
            Spacer(modifier = Modifier.height(6.dp))
            VitalsTextField(value = state.address, onValueChange = viewModel::updateAddress, placeholder = "")
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Симптомы", style = VitalsTheme.typography.labelMedium, color = colors.textMuted)
            Spacer(modifier = Modifier.height(6.dp))
            VitalsTextField(
                value = state.symptoms,
                onValueChange = viewModel::updateSymptoms,
                placeholder = "",
                minLines = 4,
                singleLine = false,
            )

            state.errorMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = it, style = VitalsTheme.typography.bodySmall, color = colors.danger)
            }

            Spacer(modifier = Modifier.height(24.dp))
            VitalsPrimaryButton(
                text = if (state.isSubmitting) "Отправляем..." else "Отправить заявку",
                onClick = viewModel::submit,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
