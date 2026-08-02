package com.vitals.mobile.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val VitalsShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object VitalsRadius {
    val input = RoundedCornerShape(14.dp)
    val card = RoundedCornerShape(18.dp)
    val button = RoundedCornerShape(14.dp)
    val chip = RoundedCornerShape(20.dp)
    val pill = RoundedCornerShape(28.dp)
}
