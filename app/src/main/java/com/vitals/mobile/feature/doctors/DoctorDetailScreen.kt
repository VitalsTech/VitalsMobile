package com.vitals.mobile.feature.doctors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsBackTopBar
import com.vitals.mobile.core.designsystem.components.VitalsCard
import com.vitals.mobile.core.designsystem.components.VitalsPrimaryButton
import com.vitals.mobile.core.designsystem.components.VitalsSecondaryButton
import com.vitals.mobile.core.navigation.NavRoutes

@Composable
fun DoctorDetailScreen(
    doctorId: String,
    navController: NavHostController,
    viewModel: DoctorDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors

    LaunchedEffect(doctorId) { viewModel.load(doctorId) }

    val doctor = state.doctor

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsBackTopBar(title = doctor?.displayName ?: "Врач", onBack = { navController.popBackStack() })
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = doctor?.displaySpecialty ?: "Специалист",
                style = VitalsTheme.typography.bodySmall,
                color = colors.textMuted,
            )
            Spacer(modifier = Modifier.height(12.dp))
            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = doctor?.biography ?: doctor?.bio ?: doctor?.description
                        ?: "Информация о специалисте временно недоступна",
                    style = VitalsTheme.typography.bodySmall,
                    color = colors.textMuted,
                    modifier = Modifier.padding(15.dp),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            VitalsPrimaryButton(
                text = "Запись на приём",
                onClick = { navController.navigate(NavRoutes.doctorBook(doctorId)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            VitalsSecondaryButton(
                text = "Чат с врачом",
                onClick = { navController.navigate(NavRoutes.doctorChat(doctorId)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            VitalsSecondaryButton(
                text = "Онлайн-консультация",
                onClick = { navController.navigate(NavRoutes.doctorChat(doctorId)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
