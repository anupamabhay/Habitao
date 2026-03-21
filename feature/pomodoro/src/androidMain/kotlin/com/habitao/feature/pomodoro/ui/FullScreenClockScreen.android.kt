package com.habitao.feature.pomodoro.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
actual fun EnterImmersiveMode() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    DisposableEffect(activity) {
        activity?.let { targetActivity ->
            val controller =
                WindowCompat.getInsetsController(
                    targetActivity.window,
                    targetActivity.window.decorView,
                )
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            targetActivity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // AOD-style: dim screen brightness to minimum
            val layoutParams = targetActivity.window.attributes
            layoutParams.screenBrightness = AOD_BRIGHTNESS
            targetActivity.window.attributes = layoutParams
        }

        onDispose {
            activity?.let { targetActivity ->
                val controller =
                    WindowCompat.getInsetsController(
                        targetActivity.window,
                        targetActivity.window.decorView,
                    )
                controller.show(WindowInsetsCompat.Type.systemBars())
                targetActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                // Restore system brightness
                val layoutParams = targetActivity.window.attributes
                layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                targetActivity.window.attributes = layoutParams
            }
        }
    }
}

/** AOD brightness: very dim but still visible on OLED */
private const val AOD_BRIGHTNESS = 0.01f

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled, onBack)
}
