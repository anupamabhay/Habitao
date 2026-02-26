package com.habitao.feature.pomodoro.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.habitao.feature.pomodoro.ui.components.TimerDisplay
import com.habitao.feature.pomodoro.viewmodel.PomodoroViewModel

@Composable
fun FullScreenClockScreen(
    onClose: () -> Unit,
    viewModel: PomodoroViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    EnterImmersiveMode()
    BackHandler(onBack = onClose)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
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

        TimerDisplay(
            remainingSeconds = state.remainingSeconds,
            totalSeconds = state.totalSeconds,
            sessionType = state.currentSessionType,
            timerState = state.timerState,
            modifier = Modifier.size(360.dp)
        )
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
