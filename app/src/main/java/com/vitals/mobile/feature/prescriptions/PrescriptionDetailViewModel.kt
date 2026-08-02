package com.vitals.mobile.feature.prescriptions

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.common.LabLabels
import com.vitals.mobile.core.data.common.ScheduleSlotLabels
import com.vitals.mobile.core.data.prescriptions.PrescriptionDto
import com.vitals.mobile.core.data.prescriptions.PrescriptionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrescriptionDetailUiState(
    val isLoading: Boolean = true,
    val prescription: PrescriptionDto? = null,
    val instructions: String? = null,
    val statusLabel: String = "",
    val showQr: Boolean = false,
    val qrLoading: Boolean = false,
    val qrBitmap: Bitmap? = null,
    val qrPayload: String? = null,
    val qrError: String? = null,
    val actionBusy: Boolean = false,
    val actionMessage: String? = null,
    val actionError: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class PrescriptionDetailViewModel @Inject constructor(
    private val prescriptionsRepository: PrescriptionsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrescriptionDetailUiState())
    val uiState = _uiState.asStateFlow()

    fun load(prescriptionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val detail = prescriptionsRepository.get(prescriptionId)
                val instructions = runCatching {
                    prescriptionsRepository.getInstructionsText(prescriptionId)
                }.getOrNull()
                detail to instructions
            }.onSuccess { (detail, instructions) ->
                _uiState.value = PrescriptionDetailUiState(
                    isLoading = false,
                    prescription = detail,
                    instructions = instructions,
                    statusLabel = LabLabels.prescriptionStatus(detail.status),
                )
            }.onFailure { error ->
                _uiState.value = PrescriptionDetailUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Не удалось загрузить рецепт",
                )
            }
        }
    }

    fun showQr() {
        val id = _uiState.value.prescription?.resolvedId ?: return
        if (!_uiState.value.prescription!!.canShowQr()) return
        _uiState.value = _uiState.value.copy(
            showQr = true,
            qrLoading = true,
            qrError = null,
            qrBitmap = null,
            qrPayload = null,
        )
        viewModelScope.launch {
            val result = prescriptionsRepository.loadQr(id)
            _uiState.value = _uiState.value.copy(
                qrLoading = false,
                qrBitmap = result.bitmap,
                qrPayload = result.payloadPreview,
                qrError = result.errorMessage,
            )
        }
    }

    fun hideQr() {
        _uiState.value = _uiState.value.copy(showQr = false)
    }

    fun retryQr() = showQr()

    fun sendToPharmacy() {
        val id = _uiState.value.prescription?.resolvedId ?: return
        _uiState.value = _uiState.value.copy(actionBusy = true, actionError = null, actionMessage = null)
        viewModelScope.launch {
            runCatching { prescriptionsRepository.sendToPharmacy(id) }
                .onSuccess { updated ->
                    _uiState.value = _uiState.value.copy(
                        actionBusy = false,
                        prescription = updated,
                        statusLabel = LabLabels.prescriptionStatus(updated.status),
                        actionMessage = "Рецепт отправлен в ближайшую аптеку.",
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        actionBusy = false,
                        actionError = error.message ?: "Не удалось отправить в аптеку",
                    )
                }
        }
    }

    fun cancel(reason: String) {
        val id = _uiState.value.prescription?.resolvedId ?: return
        if (reason.isBlank()) return
        _uiState.value = _uiState.value.copy(actionBusy = true, actionError = null, actionMessage = null)
        viewModelScope.launch {
            runCatching { prescriptionsRepository.cancel(id, reason.trim()) }
                .onSuccess {
                    val refreshed = runCatching { prescriptionsRepository.get(id) }.getOrNull()
                    _uiState.value = _uiState.value.copy(
                        actionBusy = false,
                        prescription = refreshed ?: _uiState.value.prescription?.copy(status = "Cancelled"),
                        statusLabel = LabLabels.prescriptionStatus(refreshed?.status ?: "Cancelled"),
                        actionMessage = "Рецепт отменён.",
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        actionBusy = false,
                        actionError = error.message ?: "Не удалось отменить рецепт",
                    )
                }
        }
    }

    fun formatDate(iso: String?): String = ScheduleSlotLabels.formatDayTime(iso)
}
