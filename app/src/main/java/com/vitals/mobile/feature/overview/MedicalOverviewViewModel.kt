package com.vitals.mobile.feature.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.consultations.ConsultationProtocolDto
import com.vitals.mobile.core.data.consultations.ConsultationsRepository
import com.vitals.mobile.core.data.medicalrecords.DiagnosisDto
import com.vitals.mobile.core.data.medicalrecords.MedicalRecordsRepository
import com.vitals.mobile.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiagnosisLineUi(
    val id: String,
    val code: String,
    val title: String,
) {
    val label: String
        get() = when {
            code.isNotBlank() && code != "—" && title.isNotBlank() && !title.equals(code, true) ->
                "$code — $title"
            title.isNotBlank() -> title
            code.isNotBlank() && code != "—" -> code
            else -> "Диагноз"
        }
}

data class MedicalOverviewUiState(
    val isLoading: Boolean = true,
    val summary: String = "",
    val allergies: String = "не указаны",
    val bloodType: String = "не указана",
    val diagnoses: List<DiagnosisLineUi> = emptyList(),
    val medications: List<String> = emptyList(),
    val recentLabs: List<String> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class MedicalOverviewViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val medicalRecordsRepository: MedicalRecordsRepository,
    private val consultationsRepository: ConsultationsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicalOverviewUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun reload() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val session = sessionManager.currentSession()
            val aliases = listOfNotNull(session.patientId, session.publicId).distinct()
            if (aliases.isEmpty()) {
                _uiState.value = MedicalOverviewUiState(
                    isLoading = false,
                    errorMessage = "Профиль пациента не найден",
                )
                return@launch
            }

            val state = runCatching { medicalRecordsRepository.getStateAliases(aliases) }.getOrNull()
            val (history, historyState) = runCatching {
                medicalRecordsRepository.getHistoryBundleAliases(aliases)
            }.getOrElse { emptyList<com.vitals.mobile.core.data.medicalrecords.MedicalRecordEventDto>() to null }

            val mergedState = state ?: historyState
            val mine = runCatching { consultationsRepository.mine(includeCompleted = true, limit = 30) }
                .getOrElse { emptyList() }
            val latestProtocol = mine
                .sortedByDescending { it.completedAt ?: it.createdAt.orEmpty() }
                .firstOrNull { it.protocol != null }
                ?.protocol

            val summary = mergedState?.summary?.trim()?.takeIf { it.isNotEmpty() }
                ?: summaryFromProtocol(latestProtocol)
                ?: "Сводка появится после первой консультации или триажа."

            val diagnoses = diagnosesFromState(mergedState?.activeDiagnoses)
                .ifEmpty { diagnosesFromHistory(history) }
                .ifEmpty { diagnosesFromProtocol(latestProtocol) }

            _uiState.value = MedicalOverviewUiState(
                isLoading = false,
                summary = summary,
                allergies = mergedState?.allergies?.trim()?.takeIf { it.isNotEmpty() } ?: "не указаны",
                bloodType = mergedState?.bloodType?.trim()?.takeIf { it.isNotEmpty() } ?: "не указана",
                diagnoses = diagnoses,
                medications = mergedState?.activeMedications.orEmpty(),
                recentLabs = mergedState?.recentLabResults.orEmpty(),
            )
        }
    }

    private fun diagnosesFromState(items: List<DiagnosisDto>?): List<DiagnosisLineUi> =
        items.orEmpty().mapIndexed { index, d ->
            DiagnosisLineUi(
                id = d.sourceEventId ?: d.resolvedCode.ifBlank { "state-$index" },
                code = d.resolvedCode.ifBlank { "—" },
                title = d.resolvedTitle,
            )
        }

    private fun diagnosesFromHistory(
        events: List<com.vitals.mobile.core.data.medicalrecords.MedicalRecordEventDto>,
    ): List<DiagnosisLineUi> {
        val byCode = linkedMapOf<String, DiagnosisLineUi>()
        for (event in events) {
            val map = medicalRecordsRepository.parsePayloadMap(event)
            val type = event.eventType.orEmpty()
            if (!type.equals("diagnosis", true) && !type.equals("DiagnosisConfirmed", true)) continue
            val code = map["icd10Code"] ?: map["code"] ?: "без кода"
            val title = map["description"] ?: map["title"] ?: "Диагноз"
            byCode[code] = DiagnosisLineUi(
                id = event.resolvedId.ifBlank { code },
                code = code,
                title = title,
            )
        }
        return byCode.values.toList()
    }

    private fun diagnosesFromProtocol(protocol: ConsultationProtocolDto?): List<DiagnosisLineUi> {
        if (protocol == null) return emptyList()
        val code = protocol.preliminaryDiagnosisIcd10?.trim().orEmpty()
        val title = protocol.preliminaryDiagnosisText?.trim().orEmpty()
        if (code.isEmpty() && title.isEmpty()) return emptyList()
        return listOf(
            DiagnosisLineUi(
                id = "protocol",
                code = code.ifBlank { "—" },
                title = title.ifBlank { "Диагноз из консультации" },
            ),
        )
    }

    private fun summaryFromProtocol(protocol: ConsultationProtocolDto?): String? {
        if (protocol == null) return null
        val diagnosis = listOfNotNull(
            protocol.preliminaryDiagnosisIcd10?.trim()?.takeIf { it.isNotEmpty() },
            protocol.preliminaryDiagnosisText?.trim()?.takeIf { it.isNotEmpty() },
        ).joinToString(" ")
        if (diagnosis.isNotBlank()) return diagnosis.take(180)
        protocol.complaints?.trim()?.takeIf { it.isNotEmpty() }?.let { return it.take(180) }
        protocol.recommendations?.trim()?.takeIf { it.isNotEmpty() }?.let { return it.take(180) }
        return null
    }
}
