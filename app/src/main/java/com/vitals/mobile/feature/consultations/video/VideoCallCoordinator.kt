package com.vitals.mobile.feature.consultations.video

import android.content.Context
import android.media.AudioManager
import com.vitals.mobile.core.data.consultations.ConsultationHubClient
import com.vitals.mobile.core.data.consultations.ConsultationHubEvent
import com.vitals.mobile.core.data.consultations.ConsultationsRepository
import com.vitals.mobile.core.data.consultations.IceServerDto
import com.vitals.mobile.core.data.consultations.RtcSignal
import com.vitals.mobile.core.data.consultations.urlList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

enum class VideoPhase { Idle, Starting, InCall, Stopping, Denied }

data class VideoCallUiState(
    val phase: VideoPhase = VideoPhase.Idle,
    val incomingInvite: Boolean = false,
    val error: String? = null,
    val sfuUnavailable: Boolean = false,
    val audioEnabled: Boolean = true,
    val videoEnabled: Boolean = true,
    val hasRemoteVideo: Boolean = false,
    val localSpeaking: Boolean = false,
    val remoteSpeaking: Boolean = false,
)

class VideoCallCoordinator(
    private val appContext: Context,
    private val repository: ConsultationsRepository,
    val hub: ConsultationHubClient,
    private val scope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(VideoCallUiState())
    val uiState: StateFlow<VideoCallUiState> = _uiState.asStateFlow()

    private val _refreshChat = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshChat: SharedFlow<Unit> = _refreshChat.asSharedFlow()

    private val _status = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val statusChanges: SharedFlow<String> = _status.asSharedFlow()

    private var sessionId: String? = null
    private var enabled: Boolean = false
    private var eventsJob: Job? = null
    private var offerTimerJob: Job? = null

    private var eglBase: EglBase? = null
    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var remoteAudioTrack: AudioTrack? = null
    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null

    private val pendingIce = mutableListOf<IceCandidate>()
    private val localIce = mutableListOf<RtcSignal>()
    private val pendingSignals = mutableListOf<RtcSignal>()
    private var makingOffer = false
    private var initiator = false
    private var ignoreOffer = false
    private var stopping = false
    private var previousAudioMode: Int = AudioManager.MODE_NORMAL
    private var previousSpeaker: Boolean = false

    private var pendingFactoryRelease = false
    private val localSpeakingMonitor = SpeakingMonitor { speaking ->
        _uiState.value = _uiState.value.copy(localSpeaking = speaking && _uiState.value.audioEnabled)
    }
    private val remoteSpeakingMonitor = SpeakingMonitor { speaking ->
        _uiState.value = _uiState.value.copy(remoteSpeaking = speaking)
    }

    val eglContext: EglBase.Context?
        get() = eglBase?.eglBaseContext

    fun attach(sessionId: String, enabled: Boolean) {
        this.sessionId = sessionId
        this.enabled = enabled
        if (!enabled) {
            scope.launch { stopInternal(notifyPeer = false) }
            hub.disconnect()
            return
        }
        eventsJob?.cancel()
        eventsJob = scope.launch {
            hub.events.collect { event ->
                when (event) {
                    is ConsultationHubEvent.Message, ConsultationHubEvent.ClinicalAction ->
                        _refreshChat.tryEmit(Unit)
                    is ConsultationHubEvent.StatusChanged -> _status.tryEmit(event.status)
                    ConsultationHubEvent.VideoStarted -> {
                        if (_uiState.value.phase == VideoPhase.Idle) {
                            _uiState.value = _uiState.value.copy(incomingInvite = true)
                        }
                    }
                    ConsultationHubEvent.VideoStopped -> {
                        if (_uiState.value.phase != VideoPhase.Idle) {
                            teardownMedia()
                            _uiState.value = VideoCallUiState()
                        } else {
                            _uiState.value = _uiState.value.copy(incomingInvite = false)
                        }
                        _refreshChat.tryEmit(Unit)
                    }
                    is ConsultationHubEvent.Rtc -> handleSignal(event.signal)
                }
            }
        }
        scope.launch {
            hub.connect(sessionId)
            runCatching { repository.get(sessionId) }
                .onSuccess { consultation ->
                    if (consultation.isVideoActive() && _uiState.value.phase == VideoPhase.Idle) {
                        _uiState.value = _uiState.value.copy(incomingInvite = true)
                    }
                }
        }
    }

    fun start(asInitiator: Boolean, videoRecordingConsent: Boolean = false) {
        val id = sessionId ?: return
        if (!enabled) return
        val phase = _uiState.value.phase
        if (phase == VideoPhase.Starting || phase == VideoPhase.InCall) return
        scope.launch { connectCall(id, asInitiator, videoRecordingConsent) }
    }

    fun stop() {
        scope.launch { stopInternal(notifyPeer = true) }
    }

    fun toggleAudio() {
        val next = !_uiState.value.audioEnabled
        localAudioTrack?.setEnabled(next)
        if (!next) localSpeakingMonitor.reset()
        _uiState.value = _uiState.value.copy(audioEnabled = next, localSpeaking = false)
        hub.sendRtcSignal(RtcSignal(type = "media", audio = next, video = _uiState.value.videoEnabled))
    }

    fun toggleVideo() {
        val next = !_uiState.value.videoEnabled
        localVideoTrack?.setEnabled(next)
        _uiState.value = _uiState.value.copy(videoEnabled = next)
        hub.sendRtcSignal(RtcSignal(type = "media", audio = _uiState.value.audioEnabled, video = next))
    }

    fun bindLocal(renderer: SurfaceViewRenderer) {
        localRenderer = renderer
        localVideoTrack?.addSink(renderer)
    }

    fun unbindLocal(renderer: SurfaceViewRenderer) {
        localVideoTrack?.removeSink(renderer)
        if (localRenderer === renderer) localRenderer = null
        releaseFactoryIfIdle()
    }

    fun bindRemote(renderer: SurfaceViewRenderer) {
        remoteRenderer = renderer
        remoteVideoTrack?.addSink(renderer)
    }

    fun unbindRemote(renderer: SurfaceViewRenderer) {
        remoteVideoTrack?.removeSink(renderer)
        if (remoteRenderer === renderer) remoteRenderer = null
        releaseFactoryIfIdle()
    }

    fun release() {
        eventsJob?.cancel()
        offerTimerJob?.cancel()
        val id = sessionId
        val wasLive = _uiState.value.phase == VideoPhase.InCall || _uiState.value.phase == VideoPhase.Starting
        if (wasLive) hub.sendRtcSignal(RtcSignal(type = "hangup"))
        teardownMedia()
        hub.disconnect()
        if (wasLive && id != null) {
            CoroutineScope(Dispatchers.IO).launch { repository.stopVideo(id) }
        }
    }

    private suspend fun connectCall(id: String, asInitiator: Boolean, videoRecordingConsent: Boolean) {
        stopping = false
        initiator = asInitiator
        _uiState.value = _uiState.value.copy(
            phase = VideoPhase.Starting,
            incomingInvite = false,
            error = null,
            sfuUnavailable = false,
        )
        try {
            runCatching { repository.consent(id, true, videoRecordingConsent) }
            withContext(Dispatchers.Main) { ensureFactory() }
            startCapture()
            val room = repository.fetchVideoRoom(id, asInitiator)
            val mode = room.mode.orEmpty().lowercase().ifBlank { "p2p" }
            if (mode == "sfu") {
                teardownMedia()
                _uiState.value = VideoCallUiState(sfuUnavailable = true)
                return
            }
            createPeerConnection(room.iceServers.orEmpty())
            val queued = pendingSignals.toList()
            pendingSignals.clear()
            queued.forEach { handleSignal(it) }
            hub.sendRtcSignal(RtcSignal(type = "media", audio = true, video = true))
            offerTimerJob?.cancel()
            offerTimerJob = scope.launch {
                delay(2000)
                val pc = peerConnection ?: return@launch
                if (pc.remoteDescription != null) return@launch
                if (initiator && pc.signalingState() == PeerConnection.SignalingState.STABLE) {
                    createOffer()
                }
            }
            _uiState.value = _uiState.value.copy(phase = VideoPhase.InCall)
        } catch (error: SecurityException) {
            teardownMedia()
            _uiState.value = VideoCallUiState(
                phase = VideoPhase.Denied,
                error = "Нет доступа к камере или микрофону. Чат доступен без видео.",
            )
        } catch (error: Exception) {
            teardownMedia()
            val denied = error.message?.contains("Camera", ignoreCase = true) == true ||
                error.message?.contains("permission", ignoreCase = true) == true
            _uiState.value = if (denied) {
                VideoCallUiState(
                    phase = VideoPhase.Denied,
                    error = "Не удалось включить камеру. Чат доступен без видео.",
                )
            } else {
                VideoCallUiState(error = error.message ?: "Не удалось начать видео.")
            }
        }
    }

    private suspend fun stopInternal(notifyPeer: Boolean) {
        if (_uiState.value.phase == VideoPhase.Idle || _uiState.value.phase == VideoPhase.Stopping) {
            _uiState.value = _uiState.value.copy(incomingInvite = false)
            return
        }
        stopping = true
        _uiState.value = _uiState.value.copy(phase = VideoPhase.Stopping, incomingInvite = false)
        if (notifyPeer) hub.sendRtcSignal(RtcSignal(type = "hangup"))
        val id = sessionId
        teardownMedia()
        if (notifyPeer && id != null) repository.stopVideo(id)
        _uiState.value = VideoCallUiState()
        _refreshChat.tryEmit(Unit)
    }

    private fun handleSignal(signal: RtcSignal) {
        if (signal.type == "hangup") {
            if (_uiState.value.phase == VideoPhase.Idle) return
            stopping = true
            teardownMedia()
            _uiState.value = VideoCallUiState()
            return
        }

        val pc = peerConnection
        if (pc == null) {
            pendingSignals += signal
            return
        }

        when (signal.type) {
            "media" -> negotiateOnPeerReady(pc)
            "ice" -> {
                val candidate = IceCandidate(
                    signal.sdpMid,
                    signal.sdpMLineIndex ?: 0,
                    signal.candidate.orEmpty(),
                )
                if (pc.remoteDescription == null) {
                    pendingIce += candidate
                } else {
                    pc.addIceCandidate(candidate)
                }
            }
            "offer" -> {
                val sdp = signal.sdp ?: return
                val offerCollision = makingOffer || pc.signalingState() != PeerConnection.SignalingState.STABLE
                ignoreOffer = false
                fun answerOffer() {
                    pc.setRemoteDescription(
                        object : SdpObserver {
                            override fun onCreateSuccess(description: SessionDescription?) {}
                            override fun onSetSuccess() {
                                flushIce(pc)
                                offerTimerJob?.cancel()
                                pc.createAnswer(
                                    object : SdpObserver {
                                        override fun onCreateSuccess(description: SessionDescription?) {
                                            if (description == null) return
                                            pc.setLocalDescription(
                                                object : SdpObserver {
                                                    override fun onCreateSuccess(p0: SessionDescription?) {}
                                                    override fun onSetSuccess() {
                                                        hub.sendRtcSignal(
                                                            RtcSignal(type = "answer", sdp = description.description),
                                                        )
                                                    }
                                                    override fun onCreateFailure(error: String?) {}
                                                    override fun onSetFailure(error: String?) {}
                                                },
                                                description,
                                            )
                                        }
                                        override fun onSetSuccess() {}
                                        override fun onCreateFailure(error: String?) {}
                                        override fun onSetFailure(error: String?) {}
                                    },
                                    mediaConstraints(),
                                )
                            }
                            override fun onCreateFailure(error: String?) {}
                            override fun onSetFailure(error: String?) {}
                        },
                        SessionDescription(SessionDescription.Type.OFFER, sdp),
                    )
                }
                if (offerCollision) {
                    pc.setLocalDescription(
                        object : SdpObserver {
                            override fun onCreateSuccess(description: SessionDescription?) {}
                            override fun onSetSuccess() { answerOffer() }
                            override fun onCreateFailure(error: String?) { answerOffer() }
                            override fun onSetFailure(error: String?) { answerOffer() }
                        },
                        SessionDescription(SessionDescription.Type.ROLLBACK, ""),
                    )
                } else {
                    answerOffer()
                }
            }
            "answer" -> {
                if (ignoreOffer) return
                val sdp = signal.sdp ?: return
                pc.setRemoteDescription(
                    object : SdpObserver {
                        override fun onCreateSuccess(description: SessionDescription?) {}
                        override fun onSetSuccess() {
                            flushIce(pc)
                            offerTimerJob?.cancel()
                        }
                        override fun onCreateFailure(error: String?) {}
                        override fun onSetFailure(error: String?) {}
                    },
                    SessionDescription(SessionDescription.Type.ANSWER, sdp),
                )
            }
        }
    }

    private fun negotiateOnPeerReady(pc: PeerConnection) {
        if (pc.remoteDescription != null) return
        if (pc.signalingState() == PeerConnection.SignalingState.HAVE_LOCAL_OFFER) {
            val sdp = pc.localDescription?.description
            if (!sdp.isNullOrBlank()) {
                hub.sendRtcSignal(RtcSignal(type = "offer", sdp = sdp))
                localIce.forEach { hub.sendRtcSignal(it) }
            }
        }
    }

    private fun ensureFactory() {
        if (factory != null) return
        synchronized(factoryLock) {
            if (!factoryInitialized) {
                PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(appContext)
                        .setEnableInternalTracer(false)
                        .createInitializationOptions(),
                )
                factoryInitialized = true
            }
        }
        val egl = EglBase.create()
        eglBase = egl
        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(egl.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(egl.eglBaseContext))
            .createPeerConnectionFactory()
    }

    private fun startCapture() {
        val currentFactory = factory ?: error("WebRTC factory is not ready")
        val enumerator = Camera2Enumerator(appContext)
        val cameraName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?: enumerator.deviceNames.firstOrNull()
            ?: throw IllegalStateException("Камера не найдена")
        val capturer = enumerator.createCapturer(cameraName, object : CameraVideoCapturer.CameraEventsHandler {
            override fun onCameraError(error: String?) {}
            override fun onCameraDisconnected() {}
            override fun onCameraFreezed(error: String?) {}
            override fun onCameraOpening(name: String?) {}
            override fun onFirstFrameAvailable() {}
            override fun onCameraClosed() {}
        })
        videoCapturer = capturer
        val helper = SurfaceTextureHelper.create("VitalsCapture", eglBase!!.eglBaseContext)
        surfaceHelper = helper
        val source = currentFactory.createVideoSource(capturer.isScreencast)
        videoSource = source
        capturer.initialize(helper, appContext, source.capturerObserver)
        capturer.startCapture(1280, 720, 30)
        val videoTrack = currentFactory.createVideoTrack("vitals-video", source)
        localVideoTrack = videoTrack
        localRenderer?.let { videoTrack.addSink(it) }

        val audio = currentFactory.createAudioSource(MediaConstraints())
        audioSource = audio
        val audioTrack = currentFactory.createAudioTrack("vitals-audio", audio)
        audioTrack.setEnabled(true)
        localAudioTrack = audioTrack
        localSpeakingMonitor.attach(audioTrack)
        applyInCallAudio(true)
    }

    private fun createPeerConnection(iceServers: List<IceServerDto>) {
        val rtcIce = iceServers.mapNotNull { server ->
            val urls = server.urlList()
            if (urls.isEmpty()) return@mapNotNull null
            val builder = PeerConnection.IceServer.builder(urls)
            if (!server.username.isNullOrBlank()) builder.setUsername(server.username)
            if (!server.credential.isNullOrBlank()) builder.setPassword(server.credential)
            builder.createIceServer()
        }
        val config = PeerConnection.RTCConfiguration(rtcIce).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        val pc = factory!!.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate == null) return
                val payload = RtcSignal(
                    type = "ice",
                    candidate = candidate.sdp,
                    sdpMid = candidate.sdpMid,
                    sdpMLineIndex = candidate.sdpMLineIndex,
                )
                localIce += payload
                hub.sendRtcSignal(payload)
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: org.webrtc.MediaStream?) {}
            override fun onRemoveStream(stream: org.webrtc.MediaStream?) {}
            override fun onDataChannel(channel: org.webrtc.DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: org.webrtc.RtpReceiver?, streams: Array<out org.webrtc.MediaStream>?) {
                bindIncomingTrack(receiver?.track())
            }
            override fun onTrack(transceiver: RtpTransceiver?) {
                bindIncomingTrack(transceiver?.receiver?.track())
            }
        }) ?: error("Не удалось создать соединение")
        localAudioTrack?.let { pc.addTrack(it, listOf("vitals")) }
        localVideoTrack?.let { pc.addTrack(it, listOf("vitals")) }
        peerConnection = pc
    }

    private fun bindIncomingTrack(track: org.webrtc.MediaStreamTrack?) {
        when (track) {
            is VideoTrack -> bindRemoteTrack(track)
            is AudioTrack -> bindRemoteAudio(track)
        }
    }

    private fun bindRemoteAudio(track: AudioTrack) {
        if (remoteAudioTrack === track) return
        remoteSpeakingMonitor.detach(remoteAudioTrack)
        remoteAudioTrack = track
        remoteSpeakingMonitor.attach(track)
    }

    private fun bindRemoteTrack(track: VideoTrack?) {
        if (track == null) return
        scope.launch(Dispatchers.Main) {
            if (remoteVideoTrack === track) return@launch
            remoteVideoTrack?.let { current ->
                remoteRenderer?.let { renderer -> current.removeSink(renderer) }
            }
            remoteVideoTrack = track
            remoteRenderer?.let { track.addSink(it) }
            _uiState.value = _uiState.value.copy(hasRemoteVideo = true)
        }
    }

    private fun createOffer() {
        val pc = peerConnection ?: return
        if (pc.signalingState() != PeerConnection.SignalingState.STABLE) return
        makingOffer = true
        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription?) {
                if (description == null) {
                    makingOffer = false
                    return
                }
                pc.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        makingOffer = false
                        hub.sendRtcSignal(RtcSignal(type = "offer", sdp = description.description))
                    }
                    override fun onCreateFailure(error: String?) { makingOffer = false }
                    override fun onSetFailure(error: String?) { makingOffer = false }
                }, description)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) { makingOffer = false }
            override fun onSetFailure(error: String?) { makingOffer = false }
        }, mediaConstraints())
    }

    private fun flushIce(pc: PeerConnection) {
        val queued = pendingIce.toList()
        pendingIce.clear()
        queued.forEach { pc.addIceCandidate(it) }
    }

    private fun teardownMedia() {
        offerTimerJob?.cancel()
        offerTimerJob = null
        pendingIce.clear()
        localIce.clear()
        pendingSignals.clear()
        makingOffer = false
        initiator = false
        ignoreOffer = false
        applyInCallAudio(false)

        localRenderer?.let { renderer -> localVideoTrack?.removeSink(renderer) }
        remoteRenderer?.let { renderer -> remoteVideoTrack?.removeSink(renderer) }
        localSpeakingMonitor.detach(localAudioTrack)
        remoteSpeakingMonitor.detach(remoteAudioTrack)
        remoteVideoTrack = null
        remoteAudioTrack = null

        runCatching { videoCapturer?.stopCapture() }
        videoCapturer?.dispose()
        videoCapturer = null
        localVideoTrack?.dispose()
        localVideoTrack = null
        localAudioTrack?.dispose()
        localAudioTrack = null
        videoSource?.dispose()
        videoSource = null
        audioSource?.dispose()
        audioSource = null
        surfaceHelper?.dispose()
        surfaceHelper = null
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
        pendingFactoryRelease = true
        releaseFactoryIfIdle()
    }

    private fun releaseFactoryIfIdle() {
        if (!pendingFactoryRelease) return
        if (localRenderer != null || remoteRenderer != null) return
        factory?.dispose()
        factory = null
        eglBase?.release()
        eglBase = null
        pendingFactoryRelease = false
    }

    private fun applyInCallAudio(on: Boolean) {
        val manager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (on) {
            previousAudioMode = manager.mode
            previousSpeaker = manager.isSpeakerphoneOn
            manager.mode = AudioManager.MODE_IN_COMMUNICATION
            manager.isSpeakerphoneOn = true
        } else {
            manager.mode = previousAudioMode
            manager.isSpeakerphoneOn = previousSpeaker
        }
    }

    private fun mediaConstraints(): MediaConstraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
    }

    companion object {
        private val factoryLock = Any()
        @Volatile private var factoryInitialized = false
    }
}
