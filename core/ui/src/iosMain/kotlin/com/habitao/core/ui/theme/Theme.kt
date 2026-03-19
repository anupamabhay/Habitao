package com.habitao.core.ui.theme

import androidx.compose.runtime.Composable

/**
 * iOS implementation of [HabitaoTheme].
 * Delegates to [HabitaoThemeBase] without Android-specific status bar configuration.
 */
@Composable
fun HabitaoTheme(
    themeMode: String = "SYSTEM",
    // Accepted for API parity with Android; iOS has no dynamic color system.
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    HabitaoThemeBase(themeMode = themeMode, content = content)
}
