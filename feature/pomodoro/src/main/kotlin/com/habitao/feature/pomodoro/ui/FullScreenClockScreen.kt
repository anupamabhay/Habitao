package com.habitao.feature.pomodoro.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.koin.compose.viewmodel.koinViewModel
import com.habitao.domain.model.PomodoroType
import com.habitao.feature.pomodoro.ui.components.TimerDisplay
import com.habitao.feature.pomodoro.viewmodel.PomodoroViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FullScreenClockScreen(
    onClose: () -> Unit,
    viewModel: PomodoroViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 4 })

    EnterImmersiveMode()
    BackHandler(onBack = onClose)

    // Force dark color scheme for AOD — ensures visibility on black background in both themes
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
        ) {
            // Pager fills background - must be rendered FIRST (below overlays)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    when (page) {
                        0 -> {
                            TimerDisplay(
                                remainingSeconds = state.remainingSeconds,
                                totalSeconds = state.totalSeconds,
                                sessionType = state.currentSessionType,
                                timerState = state.timerState,
                                modifier = Modifier.size(400.dp),
                            )
                        }
                        1 -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                AnalogClock(
                                    remainingSeconds = state.remainingSeconds,
                                    totalSeconds = state.totalSeconds,
                                    modifier = Modifier.size(360.dp),
                                )
                                Spacer(modifier = Modifier.height(48.dp))
                                DigitalTimeText(state.remainingSeconds)
                            }
                        }
                        2 -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                PlantClock(
                                    remainingSeconds = state.remainingSeconds,
                                    totalSeconds = state.totalSeconds,
                                    modifier = Modifier.size(360.dp),
                                )
                                Spacer(modifier = Modifier.height(48.dp))
                                DigitalTimeText(state.remainingSeconds)
                            }
                        }
                        3 -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                TomatoClock(
                                    remainingSeconds = state.remainingSeconds,
                                    totalSeconds = state.totalSeconds,
                                    modifier = Modifier.size(300.dp),
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                DigitalTimeText(state.remainingSeconds)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = remember { currentDateLabel() },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
            }

            // Session type label - rendered ON TOP of pager
            val sessionTypeLabel =
                when (state.currentSessionType) {
                    PomodoroType.WORK -> "Focus"
                    PomodoroType.SHORT_BREAK -> "Short Break"
                    PomodoroType.LONG_BREAK -> "Long Break"
                }
            val sessionTypeColor =
                when (state.currentSessionType) {
                    PomodoroType.WORK -> MaterialTheme.colorScheme.primary
                    PomodoroType.SHORT_BREAK -> MaterialTheme.colorScheme.tertiary
                    PomodoroType.LONG_BREAK -> MaterialTheme.colorScheme.secondary
                }
            Text(
                text = sessionTypeLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = sessionTypeColor,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 56.dp),
            )

            // Close button - rendered ON TOP of pager (last = highest z-order)
            IconButton(
                onClick = onClose,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close full screen clock",
                    tint = Color.White.copy(alpha = 0.8f),
                )
            }

            // Page indicator dots
            Row(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pagerState.pageCount) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier =
                            Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        Color.White.copy(alpha = 0.9f)
                                    } else {
                                        Color.White.copy(alpha = 0.3f)
                                    },
                                ),
                    )
                }
            }
        }
    } // MaterialTheme
}

@Composable
fun DigitalTimeText(remainingSeconds: Long) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeString = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

    Text(
        text = timeString,
        style =
            MaterialTheme.typography.displayLarge.copy(
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
            ),
        color = Color.White,
    )
}

@Composable
fun AnalogClock(
    remainingSeconds: Long,
    totalSeconds: Long,
    modifier: Modifier = Modifier,
) {
    val progress = if (totalSeconds > 0L) remainingSeconds.toFloat() / totalSeconds else 0f
    val sweepAngle = 360f * progress
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    val animatedSweep by animateFloatAsState(
        targetValue = sweepAngle,
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
        label = "analog_sweep",
    )
    val backgroundColor = MaterialTheme.colorScheme.background

    Canvas(modifier = modifier) {
        val strokeWidth = 24.dp.toPx()
        val radius = size.minDimension / 2f - strokeWidth / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Draw track
        drawCircle(
            color = trackColor,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth),
        )

        // Draw progress arc
        drawArc(
            color = primaryColor,
            startAngle = -90f,
            sweepAngle = animatedSweep,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            size = Size(radius * 2, radius * 2),
            topLeft = Offset(center.x - radius, center.y - radius),
        )

        // Draw outer ring
        drawCircle(
            color = trackColor.copy(alpha = 0.5f),
            radius = radius + strokeWidth / 2 + 4.dp.toPx(),
            center = center,
            style = Stroke(width = 2.dp.toPx()),
        )

        // Draw tick marks
        for (i in 0 until 60) {
            val angle = i * 6f * (Math.PI / 180f)
            val isHour = i % 5 == 0
            val tickLength = if (isHour) 12.dp.toPx() else 6.dp.toPx()
            val tickStroke = if (isHour) 3.dp.toPx() else 1.5.dp.toPx()
            val tickColor = if (isHour) secondaryColor else trackColor

            val innerRadius = radius - strokeWidth / 2 - 16.dp.toPx()
            val startX = center.x + (innerRadius * cos(angle)).toFloat()
            val startY = center.y + (innerRadius * sin(angle)).toFloat()
            val endX = center.x + ((innerRadius - tickLength) * cos(angle)).toFloat()
            val endY = center.y + ((innerRadius - tickLength) * sin(angle)).toFloat()

            drawLine(
                color = tickColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = tickStroke,
                cap = StrokeCap.Round,
            )
        }

        // Draw hand
        val angleInRadians = (animatedSweep - 90f) * (Math.PI / 180f)
        val handLength = radius * 0.85f
        val handX = center.x + (handLength * cos(angleInRadians)).toFloat()
        val handY = center.y + (handLength * sin(angleInRadians)).toFloat()

        // Hand tail
        val tailLength = radius * 0.15f
        val tailX = center.x - (tailLength * cos(angleInRadians)).toFloat()
        val tailY = center.y - (tailLength * sin(angleInRadians)).toFloat()

        drawLine(
            color = secondaryColor,
            start = Offset(tailX, tailY),
            end = Offset(handX, handY),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round,
        )

        // Draw center dot
        drawCircle(
            color = secondaryColor,
            radius = 8.dp.toPx(),
            center = center,
        )
        drawCircle(
            color = backgroundColor,
            radius = 3.dp.toPx(),
            center = center,
        )
    }
}

@Composable
fun PlantClock(
    remainingSeconds: Long,
    totalSeconds: Long,
    modifier: Modifier = Modifier,
) {
    val progress = if (totalSeconds > 0L) 1f - (remainingSeconds.toFloat() / totalSeconds) else 1f
    val plantColor = MaterialTheme.colorScheme.tertiary
    val potColor = MaterialTheme.colorScheme.tertiaryContainer
    val potRimColor = MaterialTheme.colorScheme.onTertiaryContainer

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "plant_growth",
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Pot dimensions
        val potWidthTop = width * 0.45f
        val potWidthBottom = width * 0.3f
        val potHeight = height * 0.25f
        val potTop = height - potHeight

        // Draw Pot Base
        drawPath(
            path =
                Path().apply {
                    moveTo(width / 2f - potWidthTop / 2f, potTop)
                    lineTo(width / 2f + potWidthTop / 2f, potTop)
                    // Curved sides
                    quadraticTo(
                        width / 2f + potWidthTop / 2.2f,
                        height - potHeight / 2f,
                        width / 2f + potWidthBottom / 2f,
                        height,
                    )
                    lineTo(width / 2f - potWidthBottom / 2f, height)
                    quadraticTo(
                        width / 2f - potWidthTop / 2.2f,
                        height - potHeight / 2f,
                        width / 2f - potWidthTop / 2f,
                        potTop,
                    )
                    close()
                },
            color = potColor,
        )

        // Draw Pot Rim
        val rimHeight = height * 0.04f
        drawPath(
            path =
                Path().apply {
                    moveTo(width / 2f - potWidthTop / 1.8f, potTop - rimHeight)
                    lineTo(width / 2f + potWidthTop / 1.8f, potTop - rimHeight)
                    lineTo(width / 2f + potWidthTop / 1.9f, potTop)
                    lineTo(width / 2f - potWidthTop / 1.9f, potTop)
                    close()
                },
            color = potRimColor.copy(alpha = 0.8f),
        )

        // Stem
        val maxStemHeight = height * 0.65f
        val currentStemHeight = maxStemHeight * animatedProgress

        if (currentStemHeight > 0) {
            // Curvy stem
            val stemPath =
                Path().apply {
                    moveTo(width / 2f, potTop - rimHeight)
                    quadraticTo(
                        width / 2f - width * 0.05f * animatedProgress,
                        potTop - rimHeight - currentStemHeight * 0.5f,
                        width / 2f + width * 0.02f * animatedProgress,
                        potTop - rimHeight - currentStemHeight,
                    )
                }

            drawPath(
                path = stemPath,
                color = plantColor,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
            )

            // Leaves
            val numLeaves = 8
            for (i in 1..numLeaves) {
                val leafProgressThreshold = i.toFloat() / (numLeaves + 1)
                if (animatedProgress > leafProgressThreshold) {
                    val leafGrowth = (animatedProgress - leafProgressThreshold) / (1f - leafProgressThreshold)
                    val clampedGrowth = leafGrowth.coerceIn(0f, 1f)

                    // Overshoot effect for leaf pop
                    val t = clampedGrowth - 1.0f
                    val sizeAnim = (t * t * (3f * t + 2f) + 1.0f).coerceAtLeast(0f)

                    val leafSizeX = width * 0.18f * sizeAnim
                    val leafSizeY = width * 0.12f * sizeAnim

                    val yPos = potTop - rimHeight - (maxStemHeight * leafProgressThreshold)
                    val isRight = i % 2 == 0
                    val dir = if (isRight) 1f else -1f

                    // Add some curve based on position
                    val xOffset = width / 2f + (if (isRight) -width * 0.02f else width * 0.01f)

                    drawPath(
                        path =
                            Path().apply {
                                moveTo(xOffset, yPos)
                                // Bottom curve of leaf
                                cubicTo(
                                    xOffset + (leafSizeX * 0.5f * dir),
                                    yPos + leafSizeY * 0.2f,
                                    xOffset + (leafSizeX * 0.8f * dir),
                                    yPos - leafSizeY * 0.2f,
                                    xOffset + (leafSizeX * dir),
                                    yPos - leafSizeY * 0.8f,
                                )
                                // Top curve of leaf
                                cubicTo(
                                    xOffset + (leafSizeX * 0.5f * dir),
                                    yPos - leafSizeY * 1.2f,
                                    xOffset + (leafSizeX * 0.2f * dir),
                                    yPos - leafSizeY * 0.5f,
                                    xOffset,
                                    yPos,
                                )
                            },
                        color = plantColor.copy(alpha = 0.9f),
                    )
                }
            }
        }
    }
}

/**
 * Pixelated tomato timer — blocky pixel-art style. Squares start dimmed and
 * light up from top to bottom as the session progresses. When the timer finishes,
 * the entire tomato is fully lit.
 *
 * Cell types: 0 = empty, 1 = tomato body, 2 = stem, 3 = leaf/calyx.
 */
@Composable
fun TomatoClock(
    remainingSeconds: Long,
    totalSeconds: Long,
    modifier: Modifier = Modifier,
) {
    // Direct progress — no animation batching so each cell lights individually
    val progress = if (totalSeconds > 0L) 1f - (remainingSeconds.toFloat() / totalSeconds) else 1f

    // Pre-built pixel grid (17 cols x 19 rows)
    val grid = remember { buildTomatoGrid() }
    // Ordered body cells for one-cell-at-a-time fill progression.
    val bodyCellThresholds =
        remember(grid) {
            val bodyCells = mutableListOf<Pair<Int, Int>>()
            for (r in grid.indices) {
                for (c in grid[r].indices) {
                    if (grid[r][c] == 1) {
                        bodyCells.add(r to c)
                    }
                }
            }

            val totalCells = bodyCells.size.coerceAtLeast(1)
            bodyCells.mapIndexed { index, cell ->
                val threshold = index.toFloat() / totalCells.toFloat()
                cell to threshold
            }.toMap()
        }

    val tomatoLit = Color(0xFFE53935)
    val tomatoDim = Color(0xFF3A1010)
    val stemColor = Color(0xFF2E7D32)
    val leafColor = Color(0xFF43A047)

    Canvas(modifier = modifier) {
        val cols = grid[0].size
        val rows = grid.size
        val gap = 2.dp.toPx()
        val cellW = (size.width - gap * (cols - 1)) / cols
        val cellH = (size.height - gap * (rows - 1)) / rows
        val cell = minOf(cellW, cellH)
        val totalW = cols * cell + (cols - 1) * gap
        val totalH = rows * cell + (rows - 1) * gap
        val offsetX = (size.width - totalW) / 2f
        val offsetY = (size.height - totalH) / 2f

        for (r in grid.indices) {
            for (c in grid[r].indices) {
                val cellType = grid[r][c]
                if (cellType == 0) continue

                val x = offsetX + c * (cell + gap)
                val y = offsetY + r * (cell + gap)

                val color =
                    when (cellType) {
                        2 -> stemColor
                        3 -> leafColor
                        1 -> {
                            val cellThreshold = bodyCellThresholds[r to c] ?: 1f
                            if (progress >= cellThreshold) tomatoLit else tomatoDim
                        }
                        else -> Color.Transparent
                    }

                drawRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(cell, cell),
                )
            }
        }
    }
}

/** Pixel grid for a 17x19 blocky tomato. */
private fun buildTomatoGrid(): Array<IntArray> {
    // 0=empty, 1=body, 2=stem, 3=leaf
    val s = 2 // stem
    val l = 3 // leaf
    val b = 1 // body
    val e = 0 // empty

    @Suppress("ktlint:standard:no-multi-spaces")
    return arrayOf(
        intArrayOf(e, e, e, e, e, e, e, s, s, e, e, e, e, e, e, e, e),
        intArrayOf(e, e, e, e, e, e, e, s, s, e, e, e, e, e, e, e, e),
        intArrayOf(e, e, e, e, l, l, e, s, s, e, l, l, e, e, e, e, e),
        intArrayOf(e, e, e, l, l, l, l, s, s, l, l, l, l, e, e, e, e),
        intArrayOf(e, e, e, e, l, l, l, l, l, l, l, l, e, e, e, e, e),
        intArrayOf(e, e, e, e, e, b, b, b, b, b, b, e, e, e, e, e, e),
        intArrayOf(e, e, e, e, b, b, b, b, b, b, b, b, e, e, e, e, e),
        intArrayOf(e, e, e, b, b, b, b, b, b, b, b, b, b, e, e, e, e),
        intArrayOf(e, e, b, b, b, b, b, b, b, b, b, b, b, b, e, e, e),
        intArrayOf(e, b, b, b, b, b, b, b, b, b, b, b, b, b, b, e, e),
        intArrayOf(e, b, b, b, b, b, b, b, b, b, b, b, b, b, b, e, e),
        intArrayOf(b, b, b, b, b, b, b, b, b, b, b, b, b, b, b, b, e),
        intArrayOf(b, b, b, b, b, b, b, b, b, b, b, b, b, b, b, b, e),
        intArrayOf(b, b, b, b, b, b, b, b, b, b, b, b, b, b, b, b, e),
        intArrayOf(e, b, b, b, b, b, b, b, b, b, b, b, b, b, b, e, e),
        intArrayOf(e, e, b, b, b, b, b, b, b, b, b, b, b, b, e, e, e),
        intArrayOf(e, e, e, b, b, b, b, b, b, b, b, b, b, e, e, e, e),
        intArrayOf(e, e, e, e, b, b, b, b, b, b, b, b, e, e, e, e, e),
        intArrayOf(e, e, e, e, e, e, b, b, b, b, e, e, e, e, e, e, e),
    )
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


private fun currentDateLabel(): String {
    val date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val day = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$day, $month ${date.dayOfMonth}"
}
