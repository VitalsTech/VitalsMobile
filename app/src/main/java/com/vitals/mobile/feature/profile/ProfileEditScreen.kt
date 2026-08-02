package com.vitals.mobile.feature.profile

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsBackTopBar
import com.vitals.mobile.core.designsystem.components.VitalsPrimaryButton
import com.vitals.mobile.core.designsystem.components.VitalsTextField

@Composable
fun ProfileEditScreen(
    navController: NavHostController,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors

    LaunchedEffect(state.saved) {
        if (state.saved) navController.popBackStack()
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsBackTopBar(title = "Профиль", onBack = { navController.popBackStack() })
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "ФИО", style = VitalsTheme.typography.labelMedium, color = colors.textMuted)
            Spacer(modifier = Modifier.height(6.dp))
            VitalsTextField(value = state.fullName, onValueChange = viewModel::updateFullName, placeholder = "")
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Телефон", style = VitalsTheme.typography.labelMedium, color = colors.textMuted)
            Spacer(modifier = Modifier.height(6.dp))
            VitalsTextField(
                value = state.phone,
                onValueChange = viewModel::updatePhone,
                placeholder = "",
                keyboardType = KeyboardType.Phone,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Email", style = VitalsTheme.typography.labelMedium, color = colors.textMuted)
            Spacer(modifier = Modifier.height(6.dp))
            VitalsTextField(
                value = state.email,
                onValueChange = viewModel::updateEmail,
                placeholder = "",
                keyboardType = KeyboardType.Email,
            )
            Spacer(modifier = Modifier.height(24.dp))

            VitalsPrimaryButton(
                text = if (state.isSaving) "Сохраняем..." else "Сохранить",
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
