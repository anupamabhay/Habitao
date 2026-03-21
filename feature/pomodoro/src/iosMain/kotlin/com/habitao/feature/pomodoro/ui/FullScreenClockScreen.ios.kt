package com.habitao.feature.pomodoro.ui

import androidx.compose.runtime.Composable

@Composable
actual fun EnterImmersiveMode() {
    // Immersive mode is handled differently on iOS or not required.
    // E.g. UIApplication.sharedApplication.idleTimerDisabled = true
}

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op for iOS since gestures/nav controllers handle back navigation naturally
}
