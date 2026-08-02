package com.vitals.mobile.feature.labs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.laborders.LabOrderDto
import com.vitals.mobile.core.data.laborders.LabOrdersRepository
import com.vitals.mobile.core.data.prescriptions.PrescriptionDto
import com.vitals.mobile.core.data.prescriptions.PrescriptionsRepository
import com.vitals.mobile.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LabsUiState(
    val isLoading: Boolean = true,
    val labOrders: List<LabOrderDto> = emptyList(),
    val prescriptions: List<PrescriptionDto> = emptyList(),
)

@HiltViewModel
class LabsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val labOrdersRepository: LabOrdersRepository,
    private val prescriptionsRepository: PrescriptionsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LabsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val patientId = sessionManager.currentSession().patientId
            if (patientId == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }
            val labOrders = runCatching { labOrdersRepository.getForPatient(patientId) }.getOrElse { emptyList() }
            val prescriptions = runCatching { prescriptionsRepository.getForPatient(patientId) }.getOrElse { emptyList() }
            _uiState.value = LabsUiState(isLoading = false, labOrders = labOrders, prescriptions = prescriptions)
        }
    }
}
