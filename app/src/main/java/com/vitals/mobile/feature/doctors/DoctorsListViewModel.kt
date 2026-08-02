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

data class DoctorsListUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val doctors: List<DoctorDto> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class DoctorsListViewModel @Inject constructor(
    private val doctorsRepository: DoctorsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorsListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { doctorsRepository.listDoctors(query = _uiState.value.query.ifBlank { null }) }
                .onSuccess { doctors ->
                    _uiState.value = _uiState.value.copy(isLoading = false, doctors = doctors)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        doctors = emptyList(),
                        errorMessage = error.message ?: "Не удалось загрузить врачей",
                    )
                }
        }
    }
}
