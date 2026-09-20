package com.vitals.mobile.core.data.consultations

import com.vitals.mobile.core.data.common.ChatMessageDto
import com.vitals.mobile.core.network.asArrayFlexible
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsultationsRepository @Inject constructor(
    private val api: ConsultationsApi,
    private val json: Json,
) {
    suspend fun book(
        doctorId: String,
        slotId: String,
        consultationType: ConsultationType? = null,
        urgencyLevel: String? = null,
        triageSessionId: String? = null,
    ): ConsultationDto = api.book(
        BookConsultationRequest(
            doctorId = doctorId,
            slotId = slotId,
            consultationType = consultationType?.wireValue,
            urgencyLevel = urgencyLevel,
            triageSessionId = triageSessionId,
        ),
    )

    suspend fun get(sessionId: String): ConsultationDto = api.get(sessionId)

    suspend fun mine(includeCompleted: Boolean = true, limit: Int? = null): List<ConsultationDto> {
        val raw = api.mine(includeCompleted, limit)
        return raw.asArrayFlexible("items", "data", "consultations").mapNotNull { element ->
            runCatching { json.decodeFromJsonElement<ConsultationDto>(element) }.getOrNull()
        }
    }

    suspend fun getMessages(id: String, afterSequence: Long? = null): List<ChatMessageDto> =
        api.getMessages(id, afterSequence, markAsRead = true)

    suspend fun sendMessage(id: String, content: String): ChatMessageDto =
        api.sendMessage(id, SendConsultationMessageRequest(content = content))

    suspend fun cancel(id: String, reason: String? = null) = api.cancel(id, CancelConsultationRequest(reason))

    suspend fun rate(id: String, score: Int, feedback: String? = null) =
        api.rate(id, RateConsultationRequest(score = score, feedback = feedback))

    suspend fun join(id: String) = api.join(id, JoinConsultationRequest())

    suspend fun consent(id: String, dataProcessing: Boolean, videoRecording: Boolean) =
        api.consent(id, ConsentRequest(dataProcessing, videoRecording))

    suspend fun startVideo(id: String): VideoRoomResponse = api.startVideo(id)

    suspend fun getVideo(id: String): VideoRoomResponse = api.getVideo(id)

    suspend fun stopVideo(id: String) {
        runCatching { api.stopVideo(id) }
    }

    suspend fun fetchVideoRoom(id: String, asInitiator: Boolean): VideoRoomResponse {
        if (asInitiator) return api.startVideo(id)
        return try {
            api.getVideo(id)
        } catch (error: retrofit2.HttpException) {
            if (error.code() == 409) api.startVideo(id) else throw error
        }
    }
}
