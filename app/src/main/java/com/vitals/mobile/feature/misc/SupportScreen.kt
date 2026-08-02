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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsBackTopBar
import com.vitals.mobile.core.designsystem.components.VitalsNavRow
import com.vitals.mobile.core.designsystem.components.VitalsPrimaryButton
import com.vitals.mobile.core.designsystem.components.VitalsTextField

@Composable
fun SupportScreen(
    navController: NavHostController,
    viewModel: SupportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsBackTopBar(title = "Поддержка", onBack = { navController.popBackStack() })
        Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
            SUPPORT_FAQ.forEach { question ->
                VitalsNavRow(title = question, onClick = { viewModel.selectFaq(question) })
                Spacer(modifier = Modifier.height(12.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            state.sentMessage?.let {
                Text(text = it, style = VitalsTheme.typography.bodySmall, color = colors.textMuted)
                Spacer(modifier = Modifier.height(8.dp))
            }
            VitalsTextField(
                value = state.question,
                onValueChange = viewModel::updateQuestion,
                placeholder = "Задать вопрос...",
                minLines = 3,
                singleLine = false,
            )
            Spacer(modifier = Modifier.height(12.dp))
            VitalsPrimaryButton(
                text = if (state.isSending) "Отправляем..." else "Отправить",
                onClick = viewModel::send,
                enabled = !state.isSending,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
