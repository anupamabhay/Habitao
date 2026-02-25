package com.habitao.feature.pomodoro.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.TextClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.habitao.feature.pomodoro.service.TimerState
import com.habitao.feature.pomodoro.viewmodel.PomodoroViewModel

@Composable
fun FullScreenClockScreen(
    onClose: () -> Unit,
    viewModel: PomodoroViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val clockTextColor = MaterialTheme.colorScheme.onBackground.toArgb()

    EnterImmersiveMode()
    BackHandler(onBack = onClose)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                TextClock(context).apply {
                    format12Hour = "h:mm"
                    format24Hour = "HH:mm"
                    gravity = Gravity.CENTER
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
                    textSize = 104f
                }
            },
            update = { textClock ->
                textClock.setTextColor(clockTextColor)
            },
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close full screen clock",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }

        val isTimerActive = state.timerState == TimerState.RUNNING || state.timerState == TimerState.PAUSED
        if (isTimerActive) {
            Text(
                text = "Pomodoro ${formatRemainingTime(state.remainingSeconds)}",
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun EnterImmersiveMode() {
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
        }

        onDispose {
            activity?.let { targetActivity ->
                val controller =
                    WindowCompat.getInsetsController(
                        targetActivity.window,
                        targetActivity.window.decorView,
                    )
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun formatRemainingTime(seconds: Long): String {
    val safeSeconds = seconds.coerceAtLeast(0L)
    val minutes = safeSeconds / 60
    val remainingSeconds = safeSeconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
