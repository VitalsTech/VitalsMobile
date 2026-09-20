package com.vitals.mobile.core.data.consultations

import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import com.microsoft.signalr.TransportEnum
import com.vitals.mobile.core.data.common.ChatMessageDto
import com.vitals.mobile.core.network.ApiConfig
import com.vitals.mobile.core.session.SessionManager
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicReference

data class RtcSignal(
    val type: String,
    val sdp: String? = null,
    val candidate: String? = null,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null,
    val audio: Boolean? = null,
    val video: Boolean? = null,
)

sealed interface ConsultationHubEvent {
    data class Message(val dto: ChatMessageDto) : ConsultationHubEvent
    data class StatusChanged(val status: String) : ConsultationHubEvent
    data object VideoStarted : ConsultationHubEvent
    data object VideoStopped : ConsultationHubEvent
    data class Rtc(val signal: RtcSignal) : ConsultationHubEvent
    data object ClinicalAction : ConsultationHubEvent
}

class ConsultationHubClient(
    private val sessionManager: SessionManager,
    private val json: Json,
) {
    private val connectionRef = AtomicReference<HubConnection?>(null)
    private val sessionRef = AtomicReference<String?>(null)

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _events = MutableSharedFlow<ConsultationHubEvent>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<ConsultationHubEvent> = _events.asSharedFlow()

    suspend fun connect(sessionId: String) {
        disconnect()
        sessionRef.set(sessionId)
        val base = ApiConfig.BASE_URL.trimEnd('/')
        val hubUrl = "$base/api/v1/consultations/hub"
        val connection = HubConnectionBuilder.create(hubUrl)
            .withAccessTokenProvider(
                Single.fromCallable { kotlinx.coroutines.runBlocking { sessionManager.getAccessTokenBlocking().orEmpty() } },
            )
            .withTransport(TransportEnum.WEBSOCKETS)
            .build()

        connection.on("messageReceived", { raw ->
            parseMessage(raw)?.let { _events.tryEmit(ConsultationHubEvent.Message(it)) }
        }, Any::class.java)
        connection.on("statusChanged", { raw ->
            val status = stringField(raw, "status", "Status").orEmpty()
            if (status.isNotBlank()) _events.tryEmit(ConsultationHubEvent.StatusChanged(status))
        }, Any::class.java)
        connection.on("videoStarted", { _ ->
            _events.tryEmit(ConsultationHubEvent.VideoStarted)
        }, Any::class.java)
        connection.on("videoStopped", { _ ->
            _events.tryEmit(ConsultationHubEvent.VideoStopped)
        }, Any::class.java)
        connection.on("rtcSignal", { raw ->
            parseRtc(raw)?.let { _events.tryEmit(ConsultationHubEvent.Rtc(it)) }
        }, Any::class.java)
        connection.on("clinicalAction", { _ ->
            _events.tryEmit(ConsultationHubEvent.ClinicalAction)
        }, Any::class.java)
        connection.onClosed { _ ->
            _ready.value = false
        }

        connectionRef.set(connection)
        try {
            withContext(Dispatchers.IO) {
                connection.start().blockingAwait()
                connection.invoke("JoinSession", sessionId).blockingAwait()
            }
            _ready.value = connection.connectionState == HubConnectionState.CONNECTED
            _error.value = null
        } catch (_: Exception) {
            _ready.value = false
            _error.value = "Нет связи с сервером звонка."
            connectionRef.compareAndSet(connection, null)
            runCatching { connection.stop() }
        }
    }

    fun sendRtcSignal(signal: RtcSignal) {
        val connection = connectionRef.get() ?: return
        val sessionId = sessionRef.get() ?: return
        if (connection.connectionState != HubConnectionState.CONNECTED) return
        val payload = LinkedHashMap<String, Any>()
        payload["type"] = signal.type
        signal.sdp?.let { payload["sdp"] = it }
        signal.candidate?.let { payload["candidate"] = it }
        signal.sdpMid?.let { payload["sdpMid"] = it }
        signal.sdpMLineIndex?.let { payload["sdpMLineIndex"] = it }
        signal.audio?.let { payload["audio"] = it }
        signal.video?.let { payload["video"] = it }
        runCatching { connection.send("SendRtcSignal", sessionId, payload) }
    }

    fun disconnect() {
        _ready.value = false
        sessionRef.set(null)
        val connection = connectionRef.getAndSet(null) ?: return
        runCatching { connection.stop().blockingAwait() }
    }

    private fun parseMessage(raw: Any?): ChatMessageDto? {
        if (raw is String) {
            return runCatching { json.decodeFromString(ChatMessageDto.serializer(), raw) }.getOrNull()
        }
        val map = asMap(raw)
        if (map.isEmpty()) return null
        return ChatMessageDto(
            id = stringField(raw, "id", "Id"),
            messageId = stringField(raw, "messageId", "MessageId"),
            sequence = longField(raw, "sequence", "sequenceNumber", "SequenceNumber"),
            role = stringField(raw, "role", "Role"),
            sender = stringField(raw, "sender", "Sender"),
            senderRole = stringField(raw, "senderRole", "SenderRole"),
            messageType = stringField(raw, "messageType", "MessageType"),
            message = stringField(raw, "message", "Message"),
            content = stringField(raw, "content", "Content"),
            text = stringField(raw, "text", "Text"),
            attachmentUrl = stringField(raw, "attachmentUrl", "AttachmentUrl"),
            isImportant = boolField(raw, "isImportant", "IsImportant"),
            sentAt = stringField(raw, "sentAt", "SentAt"),
            createdAt = stringField(raw, "createdAt", "CreatedAt"),
            timestamp = stringField(raw, "timestamp", "Timestamp"),
            readAt = stringField(raw, "readAt", "ReadAt"),
        )
    }

    private fun parseRtc(raw: Any?): RtcSignal? {
        val type = stringField(raw, "type", "Type")?.lowercase() ?: return null
        return RtcSignal(
            type = type,
            sdp = stringField(raw, "sdp", "Sdp"),
            candidate = stringField(raw, "candidate", "Candidate"),
            sdpMid = stringField(raw, "sdpMid", "SdpMid"),
            sdpMLineIndex = intField(raw, "sdpMLineIndex", "SdpMLineIndex"),
            audio = boolField(raw, "audio", "Audio"),
            video = boolField(raw, "video", "Video"),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun asMap(raw: Any?): Map<String, Any?> {
        return when (raw) {
            is Map<*, *> -> raw as Map<String, Any?>
            else -> emptyMap()
        }
    }

    private fun stringField(raw: Any?, vararg keys: String): String? {
        val map = asMap(raw)
        keys.forEach { key ->
            val value = map[key]
            if (value is String && value.isNotBlank()) return value
        }
        return null
    }

    private fun intField(raw: Any?, vararg keys: String): Int? {
        val map = asMap(raw)
        keys.forEach { key ->
            when (val value = map[key]) {
                is Number -> return value.toInt()
                is String -> value.toIntOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun longField(raw: Any?, vararg keys: String): Long? {
        val map = asMap(raw)
        keys.forEach { key ->
            when (val value = map[key]) {
                is Number -> return value.toLong()
                is String -> value.toLongOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun boolField(raw: Any?, vararg keys: String): Boolean? {
        val map = asMap(raw)
        keys.forEach { key ->
            when (val value = map[key]) {
                is Boolean -> return value
            }
        }
        return null
    }
}
