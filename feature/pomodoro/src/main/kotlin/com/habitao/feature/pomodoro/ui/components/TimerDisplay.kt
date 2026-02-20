package com.habitao.feature.pomodoro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.habitao.domain.model.PomodoroType

@Composable
fun TimerDisplay(
    remainingSeconds: Long,
    totalSeconds: Long,
    sessionType: PomodoroType,
    modifier: Modifier = Modifier,
) {
    val safeTotalSeconds = totalSeconds.coerceAtLeast(1L)
    val progress = (remainingSeconds.toFloat() / safeTotalSeconds.toFloat()).coerceIn(0f, 1f)
    val accentColor =
        if (sessionType == PomodoroType.WORK) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.tertiary
        }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(280.dp)) {
            val strokeWidth = 8.dp.toPx()

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth),
            )
            drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = -360f * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }

        Text(
            text = formatTimerText(remainingSeconds),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface,
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
