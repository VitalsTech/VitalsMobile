package com.vitals.mobile.feature.misc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.notifications.NotificationDto
import com.vitals.mobile.core.data.notifications.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val notifications: List<NotificationDto> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { notificationsRepository.getHistory() }
                .onSuccess { list ->
                    _uiState.value = NotificationsUiState(isLoading = false, notifications = list)
                }
                .onFailure { error ->
                    _uiState.value = NotificationsUiState(
                        isLoading = false,
                        errorMessage = error.message ?: "Не удалось загрузить уведомления",
                    )
                }
        }
    }
}
