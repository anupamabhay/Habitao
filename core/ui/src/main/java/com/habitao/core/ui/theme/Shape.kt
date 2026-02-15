package com.habitao.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material Design 3 Expressive Shapes
// Combining rounded corners with expressive shapes for cards and components

val Shapes = Shapes(
    // Extra Small - Used for small components like chips, buttons
    extraSmall = RoundedCornerShape(4.dp),
    
    // Small - Used for small cards, menus
    small = RoundedCornerShape(8.dp),
    
    // Medium - Used for medium cards, dialogs
    medium = RoundedCornerShape(12.dp),
    
    // Large - Used for large cards, sheets
    large = RoundedCornerShape(16.dp),
    
    // Extra Large - Used for full-screen dialogs
    extraLarge = RoundedCornerShape(28.dp)
)

// Expressive shapes for Material Design 3
// Can be used for special components like FABs or hero cards
object ExpressiveShapes {
    // Hero card shapes - larger radii for emphasis
    val heroCard = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    
    // Floating action button shape
    val fab = RoundedCornerShape(16.dp)
    
    // Bottom sheet shape
    val bottomSheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    
    // Modal shape
    val modal = RoundedCornerShape(20.dp)
}
