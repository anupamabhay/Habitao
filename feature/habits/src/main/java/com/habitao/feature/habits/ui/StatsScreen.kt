package com.habitao.feature.habits.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habitao.core.ui.theme.Dimensions
import com.habitao.feature.habits.viewmodel.ActivityDataPoint
import com.habitao.feature.habits.viewmodel.HabitStatItem
import com.habitao.feature.habits.viewmodel.StatsState
import com.habitao.feature.habits.viewmodel.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showSettingsMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                actions = {
                    Box {
                        IconButton(onClick = { showSettingsMenu = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        }
                        DropdownMenu(
                            expanded = showSettingsMenu,
                            onDismissRequest = { showSettingsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Bar Graph") },
                                onClick = {
                                    viewModel.setGraphType("BAR")
                                    showSettingsMenu = false
                                },
                                trailingIcon = if (state.graphType == "BAR") {
                                    { Icon(Icons.Outlined.CheckCircle, contentDescription = null) }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text("Line Graph") },
                                onClick = {
                                    viewModel.setGraphType("LINE")
                                    showSettingsMenu = false
                                },
                                trailingIcon = if (state.graphType == "LINE") {
                                    { Icon(Icons.Outlined.CheckCircle, contentDescription = null) }
                                } else null
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            StatsContent(
                state = state,
                onTimeFilterChanged = viewModel::onTimeFilterChanged,
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsContent(
    state: StatsState,
    onTimeFilterChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activitySubtitle =
        when (state.timeFilter) {
            0 -> "Today"
            1 -> "Past 7 days"
            else -> "Past 30 days"
        }
    val periodSuffix =
        when (state.timeFilter) {
            0 -> "today"
            1 -> "this week"
            else -> "this month"
        }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimensions.screenPaddingHorizontal),
        verticalArrangement = Arrangement.spacedBy(Dimensions.cardSpacing),
    ) {
        item(key = "top_spacer") {
            Spacer(modifier = Modifier.height(Dimensions.elementSpacing))
        }

        // Time Filter
        item(key = "time_filter") {
            val options = listOf("Day", "Week", "Month")

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        onClick = { onTimeFilterChanged(index) },
                        selected = index == state.timeFilter
                    ) {
                        Text(label)
                    }
                }
            }
        }

        item(key = "activity_graph") {
            ActivityGraphCard(
                subtitle = activitySubtitle,
                data = state.activityData,
                graphType = state.graphType,
                timeFilter = state.timeFilter,
            )
        }

        // Completion Breakdown
        item(key = "completion_breakdown") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimensions.cardSpacing),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.cardSpacing)
                ) {
                    // Tasks Breakdown
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(Dimensions.sectionSpacing),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Tasks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(Dimensions.elementSpacing))
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { state.taskCompletionRate },
                                    modifier = Modifier.size(80.dp),
                                    strokeWidth = 8.dp,
                                    strokeCap = StrokeCap.Round,
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text(
                                    text = "${(state.taskCompletionRate * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(Dimensions.elementSpacing))
                            Text(
                                text = "${state.completedTasksToday} of ${state.totalTasks} done",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Daily Goal
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(Dimensions.sectionSpacing),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Daily Goal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(Dimensions.elementSpacing))
                            Box(contentAlignment = Alignment.Center) {
                                val progress = if (state.totalHabits > 0) state.completedToday.toFloat() / state.totalHabits else 0f
                                CircularProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.size(80.dp),
                                    strokeWidth = 8.dp,
                                    strokeCap = StrokeCap.Round,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text(
                                    text = "${state.completedToday}/${state.totalHabits}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(Dimensions.elementSpacing))
                            val remaining = maxOf(0, state.totalHabits - state.completedToday)
                            Text(
                                text = "$remaining more targets to hit your goal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Routine Breakdown
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.sectionSpacing),
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.elementSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp),
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Routines",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${state.completedRoutinesToday} of ${state.totalRoutines} completed $periodSuffix",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(Dimensions.elementSpacingSmall))
                            LinearProgressIndicator(
                                progress = { state.routineCompletionRate },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }

                        Text(
                            text = "${(state.routineCompletionRate * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }

        // Current Streaks Header
        item(key = "current_streaks_header") {
            Text(
                text = "Current Streaks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(
                    top = Dimensions.sectionSpacing,
                    bottom = Dimensions.elementSpacing,
                ),
            )
        }

        // Individual habit streaks
        if (state.habitStats.isEmpty()) {
            item(key = "empty_habit_stats") {
                Text(
                    text = "No habits tracked yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(
                items = state.habitStats,
                key = { it.habitId },
            ) { habitStat ->
                CurrentStreakCard(habitStat = habitStat)
            }
        }

        // Summary Grid Header
        item(key = "summary_header") {
            Text(
                text = "Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(
                    top = Dimensions.sectionSpacing,
                    bottom = Dimensions.elementSpacing,
                ),
            )
        }

        // Summary Grid
        item(key = "summary_grid") {
            DetailedSummaryCard(state = state)
        }

        item(key = "fab_clearance") {
            Spacer(modifier = Modifier.height(Dimensions.fabClearance))
        }
    }
}

@Composable
private fun ActivityGraphCard(
    subtitle: String,
    data: List<ActivityDataPoint>,
    graphType: String,
    timeFilter: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.sectionSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimensions.elementSpacing),
        ) {
            Text(
                text = "Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (data.isEmpty()) {
                Text(
                    text = "No activity data yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val axisLabels =
                    if (timeFilter == 0) {
                        data.mapIndexed { i, p -> if (i % 3 == 0) p.label else "" }
                    } else if (data.size <= 7) {
                        data.map { it.label }
                    } else {
                        listOf(data.first().label, data[data.lastIndex / 2].label, data.last().label)
                    }

                val rawMaxValue = data.maxOf { maxOf(it.habitsCompleted, it.routinesCompleted, it.tasksCompleted) }.coerceAtLeast(1)
                val yAxisMax = calculateYAxisMax(rawMaxValue)
                val yAxisTicks = listOf(yAxisMax, yAxisMax / 2, 0)

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Sticky Y-axis (outside scroll area)
                    Column(
                        modifier = Modifier
                            .height(180.dp)
                            .padding(end = 6.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End,
                    ) {
                        yAxisTicks.forEach { tick ->
                            Text(
                                text = tick.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Scrollable chart area
                    BoxWithConstraints(modifier = Modifier.weight(1f)) {
                        val scrollState = rememberScrollState()
                        val minPointWidth = when (timeFilter) {
                            2 -> 24.dp
                            0 -> 28.dp
                            else -> 40.dp
                        }
                        val desiredWidth = (data.size * minPointWidth.value).dp
                        val chartWidth = if (desiredWidth > maxWidth) desiredWidth else maxWidth

                        Column(
                            modifier = Modifier
                                .horizontalScroll(scrollState)
                                .width(chartWidth),
                            verticalArrangement = Arrangement.spacedBy(Dimensions.elementSpacingSmall),
                        ) {
                            if (graphType == "LINE") {
                                ActivityLineChart(
                                    data = data,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                )
                            } else {
                                ActivityBarChart(
                                    data = data,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                axisLabels.forEach { label ->
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.sectionSpacing),
                ) {
                    ActivityLegend("Habits", MaterialTheme.colorScheme.primary)
                    ActivityLegend("Routines", MaterialTheme.colorScheme.secondary)
                    ActivityLegend("Tasks", MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}

@Composable
private fun ActivityLegend(
    label: String,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = color, shape = CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActivityBarChart(
    data: List<ActivityDataPoint>,
    modifier: Modifier = Modifier,
) {
    val habitsColor = MaterialTheme.colorScheme.primary
    val routinesColor = MaterialTheme.colorScheme.secondary
    val tasksColor = MaterialTheme.colorScheme.tertiary
    val rawMaxValue = maxOf(
        data.maxOfOrNull { it.habitsCompleted } ?: 0,
        data.maxOfOrNull { it.routinesCompleted } ?: 0,
        data.maxOfOrNull { it.tasksCompleted } ?: 0,
        1,
    )
    val yAxisMax = calculateYAxisMax(rawMaxValue)

    Canvas(modifier = modifier) {
        val chartTop = 8.dp.toPx()
        val chartBottom = size.height - 8.dp.toPx()
        val chartHeight = chartBottom - chartTop
        val count = data.size
        if (count == 0) return@Canvas

        val groupWidth = size.width / count
        val barWidth = (groupWidth * 0.7f) / 3f
        val gap = groupWidth * 0.3f / 2f

        data.forEachIndexed { index, point ->
            val groupLeft = index * groupWidth + gap

            fun drawBar(barIndex: Int, value: Int, color: androidx.compose.ui.graphics.Color) {
                if (value == 0) return
                val normalized = value.toFloat() / yAxisMax.toFloat()
                val barHeight = (normalized * chartHeight).coerceAtLeast(2.dp.toPx())
                val left = groupLeft + barIndex * barWidth
                val top = chartBottom - barHeight

                drawRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(width = barWidth, height = barHeight),
                )
            }

            drawBar(0, point.habitsCompleted, habitsColor)
            drawBar(1, point.routinesCompleted, routinesColor)
            drawBar(2, point.tasksCompleted, tasksColor)
        }
    }
}

@Composable
private fun ActivityLineChart(
    data: List<ActivityDataPoint>,
    modifier: Modifier = Modifier,
) {
    val habitsColor = MaterialTheme.colorScheme.primary
    val routinesColor = MaterialTheme.colorScheme.secondary
    val tasksColor = MaterialTheme.colorScheme.tertiary
    val rawMaxValue = maxOf(
        data.maxOfOrNull { it.habitsCompleted } ?: 0,
        data.maxOfOrNull { it.routinesCompleted } ?: 0,
        data.maxOfOrNull { it.tasksCompleted } ?: 0,
        1,
    )
    val yAxisMax = calculateYAxisMax(rawMaxValue)

    Canvas(modifier = modifier) {
        val chartTop = 8.dp.toPx()
        val chartBottom = size.height - 8.dp.toPx()
        val chartHeight = chartBottom - chartTop
        val count = data.size
        if (count == 0) return@Canvas
        val stepX = if (count > 1) size.width / (count - 1) else 0f

        fun point(index: Int, value: Int): Offset {
            val x = if (count > 1) index * stepX else size.width / 2f
            val normalized = value.toFloat() / yAxisMax.toFloat()
            val y = chartBottom - (normalized * chartHeight)
            return Offset(x, y)
        }

        fun drawSeries(values: List<Int>, color: androidx.compose.ui.graphics.Color) {
            val points = values.mapIndexed { index, value -> point(index, value) }
            if (points.size > 1) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val cx = (prev.x + curr.x) / 2f
                        cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                    }
                }
                drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }
            points.forEach { p -> drawCircle(color, radius = 3.dp.toPx(), center = p) }
        }

        drawSeries(data.map { it.habitsCompleted }, habitsColor)
        drawSeries(data.map { it.routinesCompleted }, routinesColor)
        drawSeries(data.map { it.tasksCompleted }, tasksColor)
    }
}

private fun calculateYAxisMax(rawMaxValue: Int): Int {
    val roundedToTen = ((rawMaxValue + 9) / 10) * 10
    return maxOf(10, roundedToTen)
}

@Composable
private fun CurrentStreakCard(
    habitStat: HabitStatItem,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.sectionSpacing),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.elementSpacing)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle, // Placeholder icon
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Column {
                    Text(
                        text = habitStat.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Target: Daily", // Placeholder subtitle
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.elementSpacing)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${habitStat.currentStreak}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                
                Icon(
                    imageVector = Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = if (habitStat.currentStreak > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailedSummaryCard(
    state: StatsState,
    modifier: Modifier = Modifier,
) {
    val periodText = when (state.timeFilter) {
        0 -> "today"
        1 -> "this week"
        else -> "this month"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.sectionSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimensions.elementSpacingLarge)
        ) {
            // Focus Insight
            if (state.todaysFocusSeconds > 0) {
                InsightRow(
                    icon = Icons.Outlined.Timer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Deep Work",
                    description = "You've logged ${formatFocusTime(state.todaysFocusSeconds)} of focus time across ${state.todaysPomodoroSessions} sessions $periodText."
                )
            }

            // Habits Insight
            val habitRate = (state.overallCompletionRate * 100).toInt()
            InsightRow(
                icon = Icons.Outlined.CheckCircle,
                iconTint = MaterialTheme.colorScheme.tertiary,
                title = "Habit Consistency",
                description = "You have a $habitRate% completion rate for your habits $periodText. Your best active streak is ${state.currentBestStreak} days."
            )

            // Tasks & Routines Insight
            val tasksText = if (state.completedTasksToday > 0) "${state.completedTasksToday} tasks" else "no tasks"
            val routinesText = if (state.completedRoutinesToday > 0) "${state.completedRoutinesToday} routines" else "no routines"
            InsightRow(
                icon = Icons.AutoMirrored.Outlined.ListAlt,
                iconTint = MaterialTheme.colorScheme.secondary,
                title = "Productivity",
                description = "You've checked off $tasksText and completed $routinesText $periodText."
            )
        }
    }
}

@Composable
private fun InsightRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.elementSpacingLarge),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconTint.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

private fun formatFocusTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    
    return if (hours > 0) {
        "${hours}.${(minutes / 6f).toInt()}h"
    } else {
        "${minutes}m"
    }
}
