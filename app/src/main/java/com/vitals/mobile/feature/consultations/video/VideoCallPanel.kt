package com.vitals.mobile.feature.consultations.video

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsPrimaryButton
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

private val SpeakingGreen = Color(0xFF23A55A)
private val ControlRed = Color(0xFFED4245)
private val ControlBg = Color(0x99000000)

@Composable
fun VideoCallPanel(
    state: VideoCallUiState,
    hubReady: Boolean,
    hubError: String?,
    eglContext: org.webrtc.EglBase.Context?,
    onStart: (videoRecordingConsent: Boolean) -> Unit,
    onJoin: (videoRecordingConsent: Boolean) -> Unit,
    onStop: () -> Unit,
    onToggleAudio: () -> Unit,
    onToggleVideo: () -> Unit,
    bindLocal: (SurfaceViewRenderer) -> Unit,
    unbindLocal: (SurfaceViewRenderer) -> Unit,
    bindRemote: (SurfaceViewRenderer) -> Unit,
    unbindRemote: (SurfaceViewRenderer) -> Unit,
    modifier: Modifier = Modifier,
    chat: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = VitalsTheme.colors
    val inCall = state.phase == VideoPhase.InCall
    var consentOpen by remember { mutableStateOf(false) }
    var pendingJoin by remember { mutableStateOf(false) }
    var recordingConsent by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(false) }
    var chatOpen by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants[Manifest.permission.CAMERA] == true &&
            grants[Manifest.permission.RECORD_AUDIO] == true
        if (granted) consentOpen = true
    }

    fun hasMediaPermission(): Boolean {
        val camera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        val audio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        return camera == PackageManager.PERMISSION_GRANTED && audio == PackageManager.PERMISSION_GRANTED
    }

    fun requestCall(join: Boolean) {
        pendingJoin = join
        if (hasMediaPermission()) {
            consentOpen = true
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    LaunchedEffect(inCall) {
        if (!inCall) {
            fullscreen = false
            chatOpen = false
        }
    }

    DisposableEffect(inCall || fullscreen) {
        view.keepScreenOn = inCall
        onDispose { view.keepScreenOn = false }
    }

    BackHandler(enabled = fullscreen) {
        if (chatOpen) chatOpen = false else fullscreen = false
    }

    @Composable
    fun Stage(stageModifier: Modifier) {
        Box(modifier = stageModifier, contentAlignment = Alignment.Center) {
            when {
                inCall && state.hasRemoteVideo && eglContext != null -> {
                    SpeakingBorder(active = state.remoteSpeaking, modifier = Modifier.fillMaxSize()) {
                        WebRtcSurface(
                            eglContext = eglContext,
                            mirror = false,
                            bind = bindRemote,
                            unbind = unbindRemote,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                inCall -> OverlayText("Ожидаем собеседника")
                state.sfuUnavailable -> OverlayText("Видео через сервер пока не подключено. Чат доступен.")
                state.phase == VideoPhase.Denied -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OverlayText(state.error ?: "Камера недоступна")
                        if (hubReady) {
                            Spacer(modifier = Modifier.height(12.dp))
                            VitalsPrimaryButton(
                                text = "Повторить",
                                onClick = { requestCall(join = false) },
                            )
                        }
                    }
                }
                state.phase == VideoPhase.Starting -> OverlayText("Подключаем камеру…")
                state.phase == VideoPhase.Stopping -> OverlayText("Завершаем видео…")
                !hubError.isNullOrBlank() -> OverlayText(hubError)
                !hubReady -> OverlayText("Подключаемся к серверу звонка…")
                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OverlayText(
                            if (state.incomingInvite) "Врач начал видеозвонок" else "Видео ещё не начато",
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        VitalsPrimaryButton(
                            text = if (state.incomingInvite) "Присоединиться" else "Начать видео",
                            onClick = { requestCall(join = state.incomingInvite) },
                        )
                    }
                }
            }

            if (inCall && eglContext != null) {
                SpeakingBorder(
                    active = state.localSpeaking && state.audioEnabled,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 72.dp)
                        .width(112.dp)
                        .height(84.dp),
                ) {
                    Box {
                        if (state.videoEnabled) {
                            WebRtcSurface(
                                eglContext = eglContext,
                                mirror = true,
                                bind = bindLocal,
                                unbind = unbindLocal,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF1A1D1C)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VideocamOff,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                )
                            }
                        }
                        if (!state.audioEnabled) {
                            Icon(
                                imageVector = Icons.Filled.MicOff,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(ControlRed)
                                    .padding(3.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun CallControls(showChat: Boolean) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0x66000000))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundControl(
                icon = if (state.audioEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
                label = if (state.audioEnabled) "Выключить микрофон" else "Включить микрофон",
                off = !state.audioEnabled,
                onClick = onToggleAudio,
            )
            RoundControl(
                icon = if (state.videoEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                label = if (state.videoEnabled) "Выключить камеру" else "Включить камеру",
                off = !state.videoEnabled,
                onClick = onToggleVideo,
            )
            RoundControl(
                icon = if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                label = if (fullscreen) "Свернуть" else "На весь экран",
                onClick = { fullscreen = !fullscreen },
            )
            if (showChat && chat != null) {
                RoundControl(
                    icon = Icons.Filled.Chat,
                    label = if (chatOpen) "Скрыть чат" else "Чат",
                    onClick = { chatOpen = !chatOpen },
                )
            }
            RoundControl(
                icon = Icons.Filled.CallEnd,
                label = "Завершить видео",
                danger = true,
                onClick = onStop,
            )
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (!fullscreen) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(VitalsTheme.shapes.card)
                    .background(Color(0xFF0B1A14)),
                contentAlignment = Alignment.Center,
            ) {
                Stage(Modifier.fillMaxSize())
                if (inCall) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                    ) {
                        CallControls(showChat = false)
                    }
                }
            }
        }

        if (!state.error.isNullOrBlank() && state.phase != VideoPhase.Denied) {
            Text(
                text = state.error,
                style = VitalsTheme.typography.bodySmall,
                color = colors.danger,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    if (fullscreen && inCall) {
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B1A14))
                    .navigationBarsPadding(),
            ) {
                Stage(Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                ) {
                    CallControls(showChat = true)
                }
                if (chatOpen && chat != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 84.dp)
                            .fillMaxWidth()
                            .fillMaxHeight(0.42f)
                            .padding(horizontal = 12.dp)
                            .clip(VitalsTheme.shapes.card)
                            .background(colors.background)
                            .padding(10.dp),
                    ) {
                        chat()
                    }
                }
            }
        }
    }

    if (consentOpen) {
        AlertDialog(
            onDismissRequest = { consentOpen = false },
            title = { Text("Согласие на видео") },
            text = {
                Column {
                    Text(
                        text = "Для звонка нужны камера и микрофон. Обработка данных консультации нужна, чтобы провести приём.",
                        style = VitalsTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = true, onCheckedChange = null, enabled = false)
                        Text("Согласие на обработку данных консультации", style = VitalsTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = recordingConsent, onCheckedChange = { recordingConsent = it })
                        Text("Согласие на запись звонка (запись сейчас не ведётся)", style = VitalsTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        consentOpen = false
                        val recording = recordingConsent
                        recordingConsent = false
                        if (pendingJoin) onJoin(recording) else onStart(recording)
                    },
                ) { Text("Продолжить") }
            },
            dismissButton = {
                TextButton(onClick = { consentOpen = false }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun RoundControl(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    off: Boolean = false,
    danger: Boolean = false,
) {
    val background = when {
        danger || off -> ControlRed
        else -> ControlBg
    }
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(background),
        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
    ) {
        Icon(imageVector = icon, contentDescription = label)
    }
}

@Composable
private fun SpeakingBorder(
    active: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(VitalsTheme.shapes.button)
            .border(3.dp, if (active) SpeakingGreen else Color.Transparent, VitalsTheme.shapes.button),
    ) {
        content()
    }
}

@Composable
private fun OverlayText(text: String) {
    Text(
        text = text,
        style = VitalsTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.85f),
        modifier = Modifier.padding(horizontal = 20.dp),
    )
}

@Composable
private fun WebRtcSurface(
    eglContext: org.webrtc.EglBase.Context,
    mirror: Boolean,
    bind: (SurfaceViewRenderer) -> Unit,
    unbind: (SurfaceViewRenderer) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                init(eglContext, null)
                setMirror(mirror)
                setEnableHardwareScaler(true)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                bind(this)
            }
        },
        onRelease = { renderer ->
            unbind(renderer)
            renderer.release()
        },
    )
}
