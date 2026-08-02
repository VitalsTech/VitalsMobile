package com.vitals.mobile.feature.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsBackTopBar
import com.vitals.mobile.core.designsystem.components.VitalsPrimaryButton
import com.vitals.mobile.core.designsystem.components.VitalsTextField

@Composable
fun DocumentNewScreen(
    navController: NavHostController,
    viewModel: DocumentNewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors

    LaunchedEffect(state.saved) {
        if (state.saved) navController.popBackStack()
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsBackTopBar(title = "Новый документ", onBack = { navController.popBackStack() })
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Название", style = VitalsTheme.typography.labelMedium, color = colors.textMuted)
            Spacer(modifier = Modifier.height(6.dp))
            VitalsTextField(value = state.title, onValueChange = viewModel::updateTitle, placeholder = "")
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Тип", style = VitalsTheme.typography.labelMedium, color = colors.textMuted)
            Spacer(modifier = Modifier.height(6.dp))
            VitalsTextField(value = state.docType, onValueChange = viewModel::updateType, placeholder = "")
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Дата", style = VitalsTheme.typography.labelMedium, color = colors.textMuted)
            Spacer(modifier = Modifier.height(6.dp))
            VitalsTextField(value = state.date, onValueChange = viewModel::updateDate, placeholder = "ДД.ММ.ГГГГ")
            Spacer(modifier = Modifier.height(24.dp))

            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .clip(VitalsTheme.shapes.card)
                    .background(colors.surface)
                    .border(1.dp, colors.border, VitalsTheme.shapes.card),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "+ PDF или скан", style = VitalsTheme.typography.bodyMedium, color = colors.textMuted)
            }

            state.errorMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = it, style = VitalsTheme.typography.bodySmall, color = colors.danger)
            }

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
