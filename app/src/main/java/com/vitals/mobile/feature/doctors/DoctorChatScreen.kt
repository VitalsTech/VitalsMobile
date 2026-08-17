package com.vitals.mobile.feature.doctors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.vitals.mobile.feature.consultations.video.VideoCallPanel

@Composable
fun DoctorChatScreen(
    doctorId: String,
    navController: NavHostController,
    viewModel: DoctorChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val video by viewModel.video.uiState.collectAsState()
    val hubReady by viewModel.video.hub.ready.collectAsState()
    val hubError by viewModel.video.hub.error.collectAsState()
    val colors = VitalsTheme.colors

    LaunchedEffect(doctorId) { viewModel.load(doctorId) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsBackTopBar(title = "Чат с врачом", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            if (state.consultationId != null && !state.closed) {
                VideoCallPanel(
                    state = video,
                    hubReady = hubReady,
                    hubError = hubError,
                    eglContext = viewModel.video.eglContext,
                    onStart = { recording -> viewModel.video.start(asInitiator = true, videoRecordingConsent = recording) },
                    onJoin = { recording -> viewModel.video.start(asInitiator = false, videoRecordingConsent = recording) },
                    onStop = viewModel.video::stop,
                    onToggleAudio = viewModel.video::toggleAudio,
                    onToggleVideo = viewModel.video::toggleVideo,
                    bindLocal = viewModel.video::bindLocal,
                    unbindLocal = viewModel.video::unbindLocal,
                    bindRemote = viewModel.video::bindRemote,
                    unbindRemote = viewModel.video::unbindRemote,
                    chat = {
                        ChatBody(
                            messages = state.messages,
                            inputText = if (state.closed) "" else state.inputText,
                            onInputChange = if (state.closed) ({}) else viewModel::updateInput,
                            onSend = { if (!state.closed) viewModel.sendCurrentInput() },
                            sendEnabled = !state.isSending && state.consultationId != null && !state.closed,
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            ChatBody(
                messages = state.messages,
                inputText = if (state.closed) "" else state.inputText,
                onInputChange = if (state.closed) ({}) else viewModel::updateInput,
                onSend = { if (!state.closed) viewModel.sendCurrentInput() },
                sendEnabled = !state.isSending && state.consultationId != null && !state.closed,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
    }
}
