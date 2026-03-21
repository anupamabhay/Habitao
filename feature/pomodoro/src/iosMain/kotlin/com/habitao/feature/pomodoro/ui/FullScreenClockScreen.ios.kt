package com.habitao.feature.pomodoro.ui

import androidx.compose.runtime.Composable

@Composable
actual fun EnterImmersiveMode() {
    // No-op on iOS; we handle full-screen by ignoring window insets in Compose
}

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op for iOS since gestures/nav controllers handle back navigation naturally
}
