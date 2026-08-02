package com.vitals.mobile.feature.treatment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.routing.RoutingRepository
import com.vitals.mobile.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TreatmentUiState(
    val isLoading: Boolean = true,
    val done: List<String> = emptyList(),
    val pending: List<String> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class TreatmentViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val routingRepository: RoutingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TreatmentUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val patientId = sessionManager.currentSession().patientId
            if (patientId == null) {
                _uiState.value = TreatmentUiState(isLoading = false)
                return@launch
            }
            runCatching { routingRepository.getActiveRoute(patientId) }
                .onSuccess { route ->
                    val steps = route.steps.orEmpty()
                    val done = steps
                        .filter {
                            it.status.equals("done", ignoreCase = true) ||
                                it.status.equals("completed", ignoreCase = true)
                        }
                        .map { it.label ?: it.action.orEmpty() }
                        .filter { it.isNotBlank() }
                    val pending = steps
                        .filterNot {
                            it.status.equals("done", ignoreCase = true) ||
                                it.status.equals("completed", ignoreCase = true)
                        }
                        .map { it.label ?: it.action.orEmpty() }
                        .filter { it.isNotBlank() }
                    _uiState.value = TreatmentUiState(isLoading = false, done = done, pending = pending)
                }
                .onFailure { error ->
                    _uiState.value = TreatmentUiState(
                        isLoading = false,
                        errorMessage = error.message ?: "Не удалось загрузить лечение",
                    )
                }
        }
    }
}
