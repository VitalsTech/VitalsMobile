package com.vitals.mobile.feature.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsMainTopBar
import com.vitals.mobile.core.designsystem.components.VitalsCard
import com.vitals.mobile.core.designsystem.components.VitalsSecondaryButton
import com.vitals.mobile.core.designsystem.components.VitalsTextField
import com.vitals.mobile.core.navigation.NavRoutes

@Composable
fun DocumentsScreen(
    navController: NavHostController,
    viewModel: DocumentsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors

    LaunchedEffect(Unit) { viewModel.load() }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            VitalsMainTopBar()
            Column(modifier = Modifier.padding(20.dp)) {
                VitalsTextField(
                    value = state.query,
                    onValueChange = viewModel::updateQuery,
                    placeholder = "Поиск документов",
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (!state.isLoading && state.filtered.isEmpty()) {
                    Text(
                        text = state.errorMessage ?: "Документов пока нет",
                        style = VitalsTheme.typography.bodyMedium,
                        color = colors.textMuted,
                    )
                } else {
                    LazyColumn {
                        items(state.filtered, key = { it.id }) { document ->
                            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                                androidx.compose.foundation.layout.Row(
                                    modifier = Modifier.padding(15.dp).fillMaxWidth(),
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(text = document.title, style = VitalsTheme.typography.titleSmall, color = colors.textPrimary)
                                        Text(text = document.docType, style = VitalsTheme.typography.bodySmall, color = colors.textMuted)
                                    }
                                    VitalsSecondaryButton(
                                        text = "Открыть",
                                        onClick = { navController.navigate(NavRoutes.documentDetail(document.id)) },
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { navController.navigate(NavRoutes.DOCUMENT_NEW) },
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(56.dp)
                .clip(CircleShape),
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Добавить документ")
        }
    }
}
