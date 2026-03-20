package com.habitao.feature.pomodoro.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.habitao.domain.model.PomodoroType
import com.habitao.feature.pomodoro.service.TimerState

@Composable
fun TimerDisplay(
    remainingSeconds: Long,
    totalSeconds: Long,
    sessionType: PomodoroType,
    timerState: TimerState = TimerState.IDLE,
    modifier: Modifier = Modifier,
) {
    val progress =
        remember(remainingSeconds, totalSeconds) {
            val safeTotalSeconds = totalSeconds.coerceAtLeast(1L)
            (remainingSeconds.toFloat() / safeTotalSeconds.toFloat()).coerceIn(0f, 1f)
        }
    val accentColor =
        if (sessionType == PomodoroType.WORK) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.tertiary
        }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulseAlpha",
    )

    val currentAlpha = if (timerState == TimerState.RUNNING) pulseAlpha else 1f

    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        val sizePx = constraints.maxWidth
        val fontSize = (sizePx * 0.10f).sp

        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = sizePx * 0.01f

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth),
            )
            drawArc(
                color = accentColor.copy(alpha = currentAlpha),
                startAngle = -90f,
                sweepAngle = -360f * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }

        Text(
            text = formatTimerText(remainingSeconds),
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = currentAlpha),
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatTimerText(seconds: Long): String {
    val safeSeconds = seconds.coerceAtLeast(0L)
    val minutesPart = safeSeconds / 60
    val secondsPart = safeSeconds % 60
    return String.format("%02d:%02d", minutesPart, secondsPart)
}
