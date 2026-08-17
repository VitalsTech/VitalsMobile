package com.vitals.mobile.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vitals.mobile.core.data.users.maskSensitive
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsCard
import com.vitals.mobile.core.designsystem.components.VitalsMainTopBar
import com.vitals.mobile.core.designsystem.components.VitalsNavRow
import com.vitals.mobile.core.designsystem.components.VitalsSecondaryButton
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
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
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
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Полис ОМС: ${maskSensitive(state.imported.insuranceNumber) ?: "-"}",
                style = VitalsTheme.typography.bodySmall,
                color = colors.textMuted,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Адрес: ${state.imported.residenceAddress ?: "-"}",
                style = VitalsTheme.typography.bodySmall,
                color = colors.textMuted,
            )

            if (state.esiaEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                VitalsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(15.dp)) {
                        Text(
                            text = "Госуслуги",
                            style = VitalsTheme.typography.titleSmall,
                            color = colors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (state.esiaStatus?.linked == true) {
                            Text(
                                text = "Госуслуги подключены",
                                style = VitalsTheme.typography.bodySmall,
                                color = colors.textMuted,
                            )
                            state.esiaStatus?.snilsMasked?.let { snils ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "СНИЛС: $snils",
                                    style = VitalsTheme.typography.bodySmall,
                                    color = colors.textMuted,
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))
                            VitalsSecondaryButton(
                                text = if (state.esiaBusy) "Подключаем Госуслуги…" else "Подключить Госуслуги",
                                onClick = viewModel::connectEsia,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.esiaBusy && state.esiaConfigured,
                            )
                        }
                        state.esiaError?.let { message ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = message,
                                style = VitalsTheme.typography.bodySmall,
                                color = colors.danger,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            VitalsNavRow(title = "Сводка и диагнозы", onClick = { navController.navigate(NavRoutes.MEDICAL_OVERVIEW) })
            Spacer(modifier = Modifier.height(12.dp))
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
