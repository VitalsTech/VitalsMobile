package com.vitals.mobile.feature.doctors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.vitals.mobile.feature.common.ChatBody

@Composable
fun DoctorChatScreen(
    doctorId: String,
    navController: NavHostController,
    viewModel: DoctorChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors

    LaunchedEffect(doctorId) { viewModel.load(doctorId) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsBackTopBar(title = "Чат с врачом", onBack = { navController.popBackStack() })
        ChatBody(
            messages = state.messages,
            inputText = state.inputText,
            onInputChange = viewModel::updateInput,
            onSend = viewModel::sendCurrentInput,
            sendEnabled = !state.isSending && state.consultationId != null,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(20.dp),
        )
    }
}
