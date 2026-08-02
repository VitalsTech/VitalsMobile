package com.vitals.mobile.feature.path

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.medicalrecords.MedicalRecordsRepository
import com.vitals.mobile.core.data.medicalrecords.MoodCode
import com.vitals.mobile.core.data.routing.RoutingRepository
import com.vitals.mobile.core.data.users.UsersRepository
import com.vitals.mobile.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RouteStepUi(
    val index: Int,
    val title: String,
    val status: String,
    val isCurrent: Boolean,
)

data class PathUiState(
    val isLoading: Boolean = true,
    val greetingName: String = "",
    val currentStepIndex: Int = 0,
    val totalSteps: Int = 0,
    val steps: List<RouteStepUi> = emptyList(),
    val infoMessage: String? = null,
)

@HiltViewModel
class PathViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val usersRepository: UsersRepository,
    private val routingRepository: RoutingRepository,
    private val medicalRecordsRepository: MedicalRecordsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PathUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val session = sessionManager.currentSession()
            val name = session.publicId?.let { publicId ->
                runCatching { usersRepository.getUser(publicId) }.getOrNull()?.let { user ->
                    user.firstName ?: user.fullName
                }
            }.orEmpty()

            val patientId = session.patientId
            val route = patientId?.let { id ->
                runCatching { routingRepository.getActiveRoute(id) }.getOrNull()
            }
            val currentIndex = route?.currentStepIndex ?: 0
            val steps = route?.steps.orEmpty().mapIndexed { index, step ->
                RouteStepUi(
                    index = index + 1,
                    title = step.label ?: step.action.orEmpty(),
                    status = step.status.orEmpty(),
                    isCurrent = index == currentIndex,
                )
            }

            _uiState.value = PathUiState(
                isLoading = false,
                greetingName = name.ifBlank { "пациент" },
                currentStepIndex = if (steps.isEmpty()) 0 else (currentIndex + 1).coerceIn(1, steps.size),
                totalSteps = steps.size,
                steps = steps,
            )
        }
    }

    fun reportFeelingWorse() {
        viewModelScope.launch {
            val id = sessionManager.currentSession().patientId ?: return@launch
            runCatching { medicalRecordsRepository.recordMoodCheck(id, MoodCode.WORSE) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(infoMessage = "Врач уведомлён о ухудшении состояния")
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(infoMessage = "Не удалось отправить уведомление")
                }
        }
    }

    fun consumeInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }
}
