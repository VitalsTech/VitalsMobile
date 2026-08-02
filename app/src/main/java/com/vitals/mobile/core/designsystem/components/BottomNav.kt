package com.vitals.mobile.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vitals.mobile.core.designsystem.VitalsTheme

data class VitalsBottomNavItem(
    val route: String,
    val label: String,
)

val vitalsBottomNavItems = listOf(
    VitalsBottomNavItem("path", "Путь"),
    VitalsBottomNavItem("triage_chat", "Триаж"),
    VitalsBottomNavItem("doctors", "Врачи"),
    VitalsBottomNavItem("profile", "Профиль"),
    VitalsBottomNavItem("more", "Ещё"),
)

@Composable
fun VitalsBottomNavBar(
    selectedRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VitalsTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(VitalsTheme.shapes.pill)
                .background(colors.surface)
                .border(1.dp, colors.border.copy(alpha = 0.7f), VitalsTheme.shapes.pill),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            vitalsBottomNavItems.forEach { item ->
                VitalsBottomNavTab(
                    item = item,
                    selected = item.route == selectedRoute,
                    onClick = { onSelect(item.route) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.VitalsBottomNavTab(
    item: VitalsBottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = VitalsTheme.colors
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 20.dp, height = 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) colors.accent else Color.Transparent),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.label,
            style = if (selected) {
                VitalsTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
            } else {
                VitalsTheme.typography.labelSmall
            },
            color = if (selected) colors.textPrimary else colors.textMuted,
        )
    }
}
