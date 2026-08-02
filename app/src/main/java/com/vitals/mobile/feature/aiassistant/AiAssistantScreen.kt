package com.vitals.mobile.feature.aiassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsMainTopBar
import com.vitals.mobile.feature.common.ChatBody

@Composable
fun AiAssistantScreen(
    navController: NavHostController,
    viewModel: AiAssistantViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsMainTopBar()
        ChatBody(
            messages = state.messages,
            inputText = state.inputText,
            onInputChange = viewModel::updateInput,
            onSend = viewModel::sendCurrentInput,
            quickReplies = listOf("Стало хуже", "Консультация", "Контроль АД"),
            onQuickReply = { text ->
                viewModel.updateInput(text)
                viewModel.sendCurrentInput()
            },
            sendEnabled = !state.isSending,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(20.dp),
        )
    }
}
