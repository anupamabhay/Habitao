package com.habitao.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material Design 3 shape scale

val Shapes =
    Shapes(
        // Chips, small buttons
        extraSmall = RoundedCornerShape(4.dp),
        // Menus, small surfaces
        small = RoundedCornerShape(8.dp),
        // Cards, dialogs
        medium = RoundedCornerShape(16.dp),
        // Habit cards, large surfaces
        large = RoundedCornerShape(20.dp),
        // Full-screen dialogs, sheets
        extraLarge = RoundedCornerShape(28.dp),
    )

// Shapes for specific components
object AppShapes {
    val fab = RoundedCornerShape(16.dp)
    val bottomSheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val dialog = RoundedCornerShape(20.dp)
    val dateChip = RoundedCornerShape(12.dp)
    val progressBar = RoundedCornerShape(4.dp)
}
