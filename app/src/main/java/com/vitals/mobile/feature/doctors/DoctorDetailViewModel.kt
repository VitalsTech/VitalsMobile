package com.vitals.mobile.feature.doctors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.doctors.DoctorDto
import com.vitals.mobile.core.data.doctors.DoctorsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DoctorDetailUiState(
    val isLoading: Boolean = true,
    val doctor: DoctorDto? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class DoctorDetailViewModel @Inject constructor(
    private val doctorsRepository: DoctorsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorDetailUiState())
    val uiState = _uiState.asStateFlow()

    fun load(doctorId: String) {
        viewModelScope.launch {
            _uiState.value = DoctorDetailUiState(isLoading = true)
            runCatching { doctorsRepository.getDoctor(doctorId) }
                .onSuccess { _uiState.value = DoctorDetailUiState(isLoading = false, doctor = it) }
                .onFailure { error ->
                    _uiState.value = DoctorDetailUiState(
                        isLoading = false,
                        errorMessage = error.message ?: "Не удалось загрузить врача",
                    )
                }
        }
    }
}
