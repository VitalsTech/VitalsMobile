package com.vitals.mobile.feature.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsBackTopBar
import com.vitals.mobile.core.designsystem.components.VitalsCard
import com.vitals.mobile.core.designsystem.components.VitalsPrimaryButton
import com.vitals.mobile.core.designsystem.components.VitalsSecondaryButton

@Composable
fun DocumentDetailScreen(
    documentId: String,
    navController: NavHostController,
    viewModel: DocumentDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors

    LaunchedEffect(documentId) { viewModel.load(documentId) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsBackTopBar(title = "Документ", onBack = { navController.popBackStack() })
        Column(modifier = Modifier.padding(20.dp)) {
            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(15.dp)) {
                    if (state.errorMessage != null) {
                        Text(text = state.errorMessage!!, style = VitalsTheme.typography.bodySmall, color = colors.danger)
                    } else {
                        Text(text = state.title.ifBlank { "Документ" }, style = VitalsTheme.typography.titleLarge, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(VitalsTheme.shapes.card)
                                .background(colors.background)
                                .border(1.dp, colors.border, VitalsTheme.shapes.card),
                        ) {}
                        Spacer(modifier = Modifier.height(12.dp))
                        if (state.note.isNotBlank()) {
                            Text(text = state.note, style = VitalsTheme.typography.bodySmall, color = colors.textMuted)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        VitalsPrimaryButton(text = "Скачать", onClick = {}, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(10.dp))
                        VitalsSecondaryButton(text = "Изменить", onClick = {}, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
