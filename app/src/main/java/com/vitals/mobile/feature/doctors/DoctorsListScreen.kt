package com.vitals.mobile.feature.doctors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vitals.mobile.core.data.doctors.DoctorDto
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsCard
import com.vitals.mobile.core.designsystem.components.VitalsMainTopBar
import com.vitals.mobile.core.designsystem.components.VitalsPrimaryButton
import com.vitals.mobile.core.designsystem.components.VitalsTextField
import com.vitals.mobile.core.navigation.NavRoutes

@Composable
fun DoctorsListScreen(
    navController: NavHostController,
    viewModel: DoctorsListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsMainTopBar()
        Column(modifier = Modifier.padding(20.dp)) {
            VitalsTextField(
                value = state.query,
                onValueChange = viewModel::updateQuery,
                placeholder = "Поиск врача",
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Врачи", style = VitalsTheme.typography.titleSmall, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            state.errorMessage?.let {
                Text(text = it, style = VitalsTheme.typography.bodySmall, color = colors.danger)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!state.isLoading && state.doctors.isEmpty()) {
                Text(
                    text = "Врачи не найдены",
                    style = VitalsTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
            } else {
                LazyColumn {
                    items(state.doctors, key = { it.resolvedId.ifBlank { it.hashCode().toString() } }) { doctor ->
                        DoctorCard(
                            doctor = doctor,
                            onClick = { navController.navigate(NavRoutes.doctorDetail(doctor.resolvedId)) },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorCard(doctor: DoctorDto, onClick: () -> Unit) {
    val colors = VitalsTheme.colors
    VitalsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(15.dp)) {
            Text(text = doctor.fullName, style = VitalsTheme.typography.titleMedium, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = doctor.specialization ?: "Врач",
                style = VitalsTheme.typography.bodySmall,
                color = colors.textMuted,
            )
            Spacer(modifier = Modifier.height(10.dp))
            VitalsPrimaryButton(text = "Подробнее", onClick = onClick, modifier = Modifier.width(140.dp))
        }
    }
}
