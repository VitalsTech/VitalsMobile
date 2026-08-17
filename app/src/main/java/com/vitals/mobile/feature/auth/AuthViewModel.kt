package com.vitals.mobile.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.auth.AuthRepository
import com.vitals.mobile.core.data.auth.DoctorProfilePayload
import com.vitals.mobile.core.data.auth.EsiaAuthOutcome
import com.vitals.mobile.core.data.auth.EsiaMessages
import com.vitals.mobile.core.data.auth.EsiaStubRegisterRequest
import com.vitals.mobile.core.data.auth.PatientProfilePayload
import com.vitals.mobile.core.data.auth.RegisterRequest
import com.vitals.mobile.core.data.auth.isStubEnabled
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

data class EsiaNotice(
    val existingAccount: Boolean,
    val devPassword: String?,
)

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
    val esiaEnabled: Boolean = false,
    val showEsiaForm: Boolean = false,
    val esiaLastName: String = "",
    val esiaFirstName: String = "",
    val esiaMiddleName: String = "",
    val esiaEmail: String = "",
    val esiaPhoneDigits: String = "",
    val esiaSubmitting: Boolean = false,
    val esiaError: String? = null,
    val esiaLastNameError: String? = null,
    val esiaFirstNameError: String? = null,
    val esiaEmailError: String? = null,
    val esiaPhoneError: String? = null,
    val esiaNotice: EsiaNotice? = null,
    val esiaPasswordCopied: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    private val displayFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private var pendingEsia: EsiaAuthOutcome? = null

    init {
        viewModelScope.launch {
            val config = authRepository.getEsiaConfig()
            _uiState.value = _uiState.value.copy(esiaEnabled = config.isStubEnabled())
        }
    }

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

    fun openEsiaForm() {
        _uiState.value = _uiState.value.copy(
            showEsiaForm = true,
            errorMessage = null,
            esiaError = null,
            esiaLastNameError = null,
            esiaFirstNameError = null,
            esiaEmailError = null,
            esiaPhoneError = null,
        )
    }

    fun closeEsiaForm() {
        pendingEsia = null
        _uiState.value = _uiState.value.copy(
            showEsiaForm = false,
            esiaNotice = null,
            esiaSubmitting = false,
            esiaError = null,
            esiaPasswordCopied = false,
        )
    }

    fun updateEsiaField(
        lastName: String? = null,
        firstName: String? = null,
        middleName: String? = null,
        email: String? = null,
        phoneRaw: String? = null,
    ) {
        val current = _uiState.value
        _uiState.value = current.copy(
            esiaLastName = lastName ?: current.esiaLastName,
            esiaFirstName = firstName ?: current.esiaFirstName,
            esiaMiddleName = middleName ?: current.esiaMiddleName,
            esiaEmail = email ?: current.esiaEmail,
            esiaPhoneDigits = phoneRaw?.let(PhoneNumber::normalizeDigits) ?: current.esiaPhoneDigits,
        )
    }

    fun submitEsia() {
        val state = _uiState.value
        val lastNameError = if (state.esiaLastName.isBlank()) "Укажите фамилию" else null
        val firstNameError = if (state.esiaFirstName.isBlank()) "Укажите имя" else null
        val emailError =
            if (state.esiaEmail.isBlank() || !state.esiaEmail.contains('@')) "Укажите корректную почту" else null
        val phoneError = if (state.esiaPhoneDigits.length < 11) "Укажите телефон" else null
        if (lastNameError != null || firstNameError != null || emailError != null || phoneError != null) {
            _uiState.value = state.copy(
                esiaLastNameError = lastNameError,
                esiaFirstNameError = firstNameError,
                esiaEmailError = emailError,
                esiaPhoneError = phoneError,
                esiaError = null,
            )
            return
        }
        _uiState.value = state.copy(
            esiaSubmitting = true,
            esiaError = null,
            esiaLastNameError = null,
            esiaFirstNameError = null,
            esiaEmailError = null,
            esiaPhoneError = null,
        )
        viewModelScope.launch {
            runCatching {
                authRepository.stubRegister(
                    EsiaStubRegisterRequest(
                        lastName = state.esiaLastName.trim(),
                        firstName = state.esiaFirstName.trim(),
                        middleName = state.esiaMiddleName.trim().ifBlank { null },
                        email = state.esiaEmail.trim(),
                        phoneNumber = state.esiaPhoneDigits,
                    ),
                )
            }.onSuccess { outcome ->
                pendingEsia = outcome
                _uiState.value = _uiState.value.copy(
                    esiaSubmitting = false,
                    showEsiaForm = false,
                    esiaNotice = EsiaNotice(
                        existingAccount = outcome.esia.existingAccount,
                        devPassword = outcome.esia.devPassword?.trim()?.ifBlank { null },
                    ),
                )
            }.onFailure { error ->
                val message = EsiaMessages.map(error)
                val lower = message.lowercase()
                _uiState.value = _uiState.value.copy(
                    esiaSubmitting = false,
                    esiaError = message,
                    esiaLastNameError = if (lower.contains("фамили") || lower.contains("имя")) message else null,
                    esiaFirstNameError = if (lower.contains("фамили") || lower.contains("имя")) message else null,
                    esiaEmailError = if (lower.contains("почт") || lower.contains("email")) message else null,
                    esiaPhoneError = if (lower.contains("телефон")) message else null,
                )
            }
        }
    }

    fun confirmEsiaNotice() {
        val outcome = pendingEsia ?: return
        _uiState.value = _uiState.value.copy(esiaSubmitting = true, esiaError = null)
        viewModelScope.launch {
            runCatching {
                authRepository.persistAuthOutcome(outcome, ProfileRole.PATIENT)
            }.onSuccess {
                pendingEsia = null
                _uiState.value = _uiState.value.copy(
                    esiaSubmitting = false,
                    esiaNotice = null,
                    isAuthenticated = true,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    esiaSubmitting = false,
                    esiaError = EsiaMessages.map(error),
                )
            }
        }
    }

    fun markEsiaPasswordCopied() {
        _uiState.value = _uiState.value.copy(esiaPasswordCopied = true)
    }

    private fun parseBirthDate(text: String): String? = try {
        LocalDate.parse(text, displayFormat).format(DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: DateTimeParseException) {
        null
    }

    private fun friendlyError(error: Throwable): String =
        error.message?.takeIf { it.isNotBlank() } ?: "Не удалось выполнить запрос. Проверьте соединение."
}
