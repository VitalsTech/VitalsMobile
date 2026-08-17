package com.vitals.mobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.auth.AuthRepository
import com.vitals.mobile.core.data.auth.EsiaMessages
import com.vitals.mobile.core.data.auth.EsiaStatusDto
import com.vitals.mobile.core.data.auth.isStubEnabled
import com.vitals.mobile.core.data.users.PatientImportedFields
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
    val imported: PatientImportedFields = PatientImportedFields(),
    val esiaEnabled: Boolean = false,
    val esiaConfigured: Boolean = true,
    val esiaStatus: EsiaStatusDto? = null,
    val esiaBusy: Boolean = false,
    val esiaError: String? = null,
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
            val config = authRepository.getEsiaConfig()
            val stub = config.isStubEnabled()
            val status = if (stub) {
                runCatching { authRepository.getEsiaStatus() }.getOrNull()
            } else {
                null
            }
            val imported = runCatching { usersRepository.getPatientImportedFields(publicId) }
                .getOrElse { PatientImportedFields() }
            runCatching { usersRepository.getUser(publicId) }
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = user,
                        imported = imported,
                        esiaEnabled = stub,
                        esiaConfigured = config.configured,
                        esiaStatus = status,
                        esiaBusy = false,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        imported = imported,
                        esiaEnabled = stub,
                        esiaConfigured = config.configured,
                        esiaStatus = status,
                        esiaBusy = false,
                    )
                }
        }
    }

    fun connectEsia() {
        if (_uiState.value.esiaBusy) return
        _uiState.value = _uiState.value.copy(esiaBusy = true, esiaError = null)
        viewModelScope.launch {
            runCatching { authRepository.stubLink() }
                .onSuccess { load() }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        esiaBusy = false,
                        esiaError = EsiaMessages.map(error),
                    )
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = _uiState.value.copy(loggedOut = true)
        }
    }
}
