package com.vitals.mobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.auth.AuthRepository
import com.vitals.mobile.core.data.users.UserDto
import com.vitals.mobile.core.data.users.UsersRepository
import com.vitals.mobile.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: UserDto? = null,
    val loggedOut: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val usersRepository: UsersRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val publicId = sessionManager.currentSession().publicId
            if (publicId == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }
            runCatching { usersRepository.getUser(publicId) }
                .onSuccess { user -> _uiState.value = _uiState.value.copy(isLoading = false, user = user) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = _uiState.value.copy(loggedOut = true)
        }
    }
}
