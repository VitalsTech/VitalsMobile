package com.vitals.mobile.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsMainTopBar
import com.vitals.mobile.core.designsystem.components.VitalsNavRow
import com.vitals.mobile.core.navigation.NavRoutes

@Composable
fun ProfileScreen(
    navController: NavHostController,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors
    val user = state.user

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsMainTopBar()
        Column(modifier = Modifier.padding(20.dp)) {
            Row {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = user?.fullName?.ifBlank { null } ?: "Пациент Vitals",
                        style = VitalsTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = listOfNotNull(user?.birthDate, user?.phoneNumber).joinToString(" · ").ifBlank { "Данные не заполнены" },
                        style = VitalsTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            VitalsNavRow(title = "Документы", onClick = { navController.navigate(NavRoutes.DOCUMENTS) })
            Spacer(modifier = Modifier.height(12.dp))
            VitalsNavRow(title = "Записаться к врачу", onClick = { navController.navigate(NavRoutes.DOCTORS) })
            Spacer(modifier = Modifier.height(12.dp))
            VitalsNavRow(title = "Редактировать профиль", onClick = { navController.navigate(NavRoutes.PROFILE_EDIT) })
            Spacer(modifier = Modifier.height(12.dp))
            VitalsNavRow(title = "Выйти", onClick = viewModel::logout)
        }
    }
}
