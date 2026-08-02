package com.vitals.mobile.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.auth.AuthRepository
import com.vitals.mobile.core.data.auth.DoctorProfilePayload
import com.vitals.mobile.core.data.auth.PatientProfilePayload
import com.vitals.mobile.core.data.auth.RegisterRequest
import com.vitals.mobile.core.session.ProfileRole
import com.vitals.mobile.core.util.PhoneNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

enum class AuthTab { LOGIN, REGISTER }

data class AuthUiState(
    val tab: AuthTab = AuthTab.LOGIN,
    val loginPhoneDigits: String = "",
    val loginPassword: String = "",
    val registerSecondName: String = "",
    val registerFirstName: String = "",
    val registerSurename: String = "",
    val registerBirthDate: String = "",
    val registerSexIsFemale: Boolean = true,
    val registerPhoneDigits: String = "",
    val registerPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    private val displayFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun selectTab(tab: AuthTab) {
        _uiState.value = _uiState.value.copy(tab = tab, errorMessage = null)
    }

    fun updateLoginPhone(raw: String) {
        _uiState.value = _uiState.value.copy(loginPhoneDigits = PhoneNumber.normalizeDigits(raw))
    }

    fun updateLoginPassword(value: String) {
        _uiState.value = _uiState.value.copy(loginPassword = value)
    }

    fun updateRegisterField(
        secondName: String? = null,
        firstName: String? = null,
        surename: String? = null,
        birthDate: String? = null,
        sexIsFemale: Boolean? = null,
        phoneRaw: String? = null,
        password: String? = null,
    ) {
        val current = _uiState.value
        _uiState.value = current.copy(
            registerSecondName = secondName ?: current.registerSecondName,
            registerFirstName = firstName ?: current.registerFirstName,
            registerSurename = surename ?: current.registerSurename,
            registerBirthDate = birthDate ?: current.registerBirthDate,
            registerSexIsFemale = sexIsFemale ?: current.registerSexIsFemale,
            registerPhoneDigits = phoneRaw?.let(PhoneNumber::normalizeDigits) ?: current.registerPhoneDigits,
            registerPassword = password ?: current.registerPassword,
        )
    }

    fun submitLogin() {
        val state = _uiState.value
        if (!PhoneNumber.isValidForApi(state.loginPhoneDigits) || state.loginPassword.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Введите телефон (10–15 цифр) и пароль")
            return
        }
        _uiState.value = state.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                authRepository.login(state.loginPhoneDigits, state.loginPassword, ProfileRole.PATIENT)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, isAuthenticated = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = friendlyError(error))
            }
        }
    }

    fun submitRegister() {
        val state = _uiState.value
        if (!PhoneNumber.isValidForApi(state.registerPhoneDigits) ||
            state.registerPassword.isBlank() ||
            state.registerBirthDate.isBlank()
        ) {
            _uiState.value = state.copy(errorMessage = "Заполните телефон, пароль и дату рождения")
            return
        }
        val isoBirthDate = parseBirthDate(state.registerBirthDate)
        if (isoBirthDate == null) {
            _uiState.value = state.copy(errorMessage = "Дата рождения в формате ДД.ММ.ГГГГ")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val payload = RegisterRequest(
                phoneNumber = state.registerPhoneDigits,
                password = state.registerPassword,
                firstName = state.registerFirstName.ifBlank { null },
                secondName = state.registerSecondName.ifBlank { null },
                surename = state.registerSurename.ifBlank { null },
                birthDate = isoBirthDate,
                sex = if (state.registerSexIsFemale) "Female" else "Male",
                patientProfile = PatientProfilePayload(),
                doctorProfile = null as DoctorProfilePayload?,
            )
            runCatching {
                authRepository.register(payload, ProfileRole.PATIENT)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, isAuthenticated = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = friendlyError(error))
            }
        }
    }

    private fun parseBirthDate(text: String): String? = try {
        LocalDate.parse(text, displayFormat).format(DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: DateTimeParseException) {
        null
    }

    private fun friendlyError(error: Throwable): String =
        error.message?.takeIf { it.isNotBlank() } ?: "Не удалось выполнить запрос. Проверьте соединение."
}
