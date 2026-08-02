package com.vitals.mobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.users.UsersRepository
import com.vitals.mobile.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileEditUiState(
    val isLoading: Boolean = true,
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val isSaving: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val usersRepository: UsersRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val publicId = sessionManager.currentSession().publicId
            if (publicId == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }
            runCatching { usersRepository.getUser(publicId) }
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        fullName = user.fullName,
                        phone = user.phoneNumber.orEmpty(),
                        email = user.email.orEmpty(),
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false) }
        }
    }

    fun updateFullName(value: String) {
        _uiState.value = _uiState.value.copy(fullName = value)
    }

    fun updatePhone(value: String) {
        _uiState.value = _uiState.value.copy(phone = value)
    }

    fun updateEmail(value: String) {
        _uiState.value = _uiState.value.copy(email = value)
    }

    fun save() {
        // NOTE: VitalsWeb's API contracts do not expose a user-profile update endpoint yet,
        // so this persists only for the current session until backend support lands.
        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
        }
    }
}
