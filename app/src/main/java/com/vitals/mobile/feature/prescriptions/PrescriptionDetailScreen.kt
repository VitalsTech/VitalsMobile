package com.vitals.mobile.feature.prescriptions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsBackTopBar
import com.vitals.mobile.core.designsystem.components.VitalsCard
import com.vitals.mobile.core.designsystem.components.VitalsPrimaryButton
import com.vitals.mobile.core.designsystem.components.VitalsSecondaryButton
import com.vitals.mobile.core.designsystem.components.VitalsTextField

@Composable
fun PrescriptionDetailScreen(
    prescriptionId: String,
    navController: NavHostController,
    viewModel: PrescriptionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors
    var cancelOpen by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("Больше не актуально") }

    LaunchedEffect(prescriptionId) { viewModel.load(prescriptionId) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsBackTopBar(title = "Рецепт", onBack = { navController.popBackStack() })
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
            return
        }
        val rx = state.prescription
        if (rx == null) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = state.errorMessage ?: "Рецепт не найден",
                    style = VitalsTheme.typography.bodyMedium,
                    color = colors.danger,
                )
            }
            return
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(
                text = rx.displayTitle(),
                style = VitalsTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = listOfNotNull(
                    state.statusLabel.takeIf { it.isNotBlank() },
                    rx.resolvedId.take(8).takeIf { it.isNotEmpty() }?.let { "№$it" },
                ).joinToString(" · "),
                style = VitalsTheme.typography.bodySmall,
                color = colors.textMuted,
            )

            state.actionError?.let {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = it, style = VitalsTheme.typography.bodySmall, color = colors.danger)
            }
            state.actionMessage?.let {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = it, style = VitalsTheme.typography.bodySmall, color = colors.success)
            }

            Spacer(modifier = Modifier.height(16.dp))
            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(15.dp)) {
                    MetaRow("Диагноз", rx.diagnosisForPrescription ?: "-")
                    MetaRow("Действует до", viewModel.formatDate(rx.validUntil))
                    MetaRow("Создан", viewModel.formatDate(rx.createdAt))
                    if (!rx.signedAt.isNullOrBlank()) {
                        MetaRow("Подписан", viewModel.formatDate(rx.signedAt))
                    }
                    if (!rx.sentToPharmacyAt.isNullOrBlank()) {
                        MetaRow("В аптеке", viewModel.formatDate(rx.sentToPharmacyAt))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Text(text = "Препараты", style = VitalsTheme.typography.titleSmall, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    val meds = rx.medications.orEmpty()
                    if (meds.isEmpty()) {
                        Text(
                            text = "Список препаратов пуст",
                            style = VitalsTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                    } else {
                        meds.forEach { med ->
                            Text(
                                text = med.resolvedName,
                                style = VitalsTheme.typography.bodyMedium,
                                color = colors.textPrimary,
                            )
                            val scheme = med.schemeLine()
                            if (scheme.isNotBlank()) {
                                Text(
                                    text = scheme,
                                    style = VitalsTheme.typography.bodySmall,
                                    color = colors.textMuted,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                                )
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            med.specialInstructions?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    text = it,
                                    style = VitalsTheme.typography.bodySmall,
                                    color = colors.textMuted,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Text(text = "Инструкция", style = VitalsTheme.typography.titleSmall, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.instructions
                            ?: "Следуйте схеме приёма. Подробная инструкция появится после подписи рецепта.",
                        style = VitalsTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            if (rx.canShowQr()) {
                VitalsPrimaryButton(
                    text = "Показать QR",
                    onClick = viewModel::showQr,
                    enabled = !state.actionBusy,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            if (rx.canSendToPharmacy()) {
                VitalsSecondaryButton(
                    text = if (state.actionBusy) "Отправка…" else "Отправить в аптеку",
                    onClick = viewModel::sendToPharmacy,
                    enabled = !state.actionBusy,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            if (rx.canCancel()) {
                VitalsSecondaryButton(
                    text = "Отменить рецепт",
                    onClick = { cancelOpen = true },
                    enabled = !state.actionBusy,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (state.showQr) {
        Dialog(onDismissRequest = viewModel::hideQr) {
            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "QR рецепта",
                        style = VitalsTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Покажите код фармацевту в аптеке-партнёре",
                        style = VitalsTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .background(colors.surfaceMuted),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            state.qrLoading -> CircularProgressIndicator(color = colors.primary)
                            state.qrError != null -> Text(
                                text = state.qrError.orEmpty(),
                                style = VitalsTheme.typography.bodySmall,
                                color = colors.danger,
                                modifier = Modifier.padding(12.dp),
                            )
                            state.qrBitmap != null -> Image(
                                bitmap = state.qrBitmap!!.asImageBitmap(),
                                contentDescription = "QR-код рецепта",
                                modifier = Modifier.size(240.dp).background(androidx.compose.ui.graphics.Color.White).padding(8.dp),
                            )
                        }
                    }
                    state.qrPayload?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = it, style = VitalsTheme.typography.labelMedium, color = colors.textMuted)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (state.qrError != null) {
                        VitalsSecondaryButton(
                            text = "Повторить",
                            onClick = viewModel::retryQr,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    VitalsSecondaryButton(
                        text = "Закрыть",
                        onClick = viewModel::hideQr,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (cancelOpen && state.prescription != null) {
        AlertDialog(
            onDismissRequest = { cancelOpen = false },
            title = { Text("Отмена рецепта") },
            text = {
                Column {
                    Text(
                        text = "${state.prescription!!.displayTitle()} · ${state.statusLabel}",
                        style = VitalsTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    VitalsTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        placeholder = "Причина",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        cancelOpen = false
                        viewModel.cancel(cancelReason)
                    },
                    enabled = cancelReason.isNotBlank() && !state.actionBusy,
                ) { Text("Подтвердить") }
            },
            dismissButton = {
                TextButton(onClick = { cancelOpen = false }) { Text("Назад") }
            },
        )
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    val colors = VitalsTheme.colors
    Text(text = label, style = VitalsTheme.typography.labelMedium, color = colors.textMuted)
    Text(
        text = value.ifBlank { "-" },
        style = VitalsTheme.typography.bodyMedium,
        color = colors.textPrimary,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}
