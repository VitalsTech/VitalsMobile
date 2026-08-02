package com.vitals.mobile.feature.misc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsCard
import com.vitals.mobile.core.designsystem.components.VitalsMainTopBar

@Composable
fun NotificationsScreen(
    navController: NavHostController,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = VitalsTheme.colors

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsMainTopBar()
        if (!state.isLoading && state.notifications.isEmpty()) {
            Text(
                text = state.errorMessage ?: "Уведомлений пока нет",
                style = VitalsTheme.typography.bodyMedium,
                color = colors.textMuted,
                modifier = Modifier.padding(20.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.padding(20.dp)) {
                items(state.notifications, key = { it.id ?: it.hashCode().toString() }) { notification ->
                    VitalsCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(15.dp).fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(text = notification.title ?: "Уведомление", style = VitalsTheme.typography.titleSmall, color = colors.textPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = notification.resolvedBody, style = VitalsTheme.typography.bodySmall, color = colors.textMuted)
                            }
                            if (notification.isRead == false) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colors.accent))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
