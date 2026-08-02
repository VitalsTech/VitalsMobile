package com.vitals.mobile.feature.misc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsMainTopBar
import com.vitals.mobile.core.designsystem.components.VitalsNavRow
import com.vitals.mobile.core.navigation.NavRoutes

private data class MoreMenuItem(val title: String, val route: String)

private val MORE_MENU_ITEMS = listOf(
    MoreMenuItem("Документы", NavRoutes.DOCUMENTS),
    MoreMenuItem("Консультации", NavRoutes.CONSULTATIONS),
    MoreMenuItem("Уведомления", NavRoutes.NOTIFICATIONS),
    MoreMenuItem("Поддержка", NavRoutes.SUPPORT),
    MoreMenuItem("Вызов на дом", NavRoutes.HOUSE_CALL),
    MoreMenuItem("Чат с ИИ", NavRoutes.TRIAGE_CHAT),
    MoreMenuItem("Лечение", NavRoutes.TREATMENT),
    MoreMenuItem("Анализы", NavRoutes.LABS),
)

@Composable
fun MoreScreen(navController: NavHostController) {
    val colors = VitalsTheme.colors

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        VitalsMainTopBar()
        Column(modifier = Modifier.padding(20.dp)) {
            MORE_MENU_ITEMS.forEach { item ->
                VitalsNavRow(title = item.title, onClick = { navController.navigate(item.route) })
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
