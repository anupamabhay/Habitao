package com.habitao.feature.routines.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habitao.core.ui.theme.Dimensions
import com.habitao.feature.routines.viewmodel.RoutineActivityPoint
import com.habitao.feature.routines.viewmodel.RoutineStatItem
import com.habitao.feature.routines.viewmodel.RoutineStatsState
import com.habitao.feature.routines.viewmodel.RoutineStatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineStatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: RoutineStatsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Routine Stats") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
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
            RoutineStatsContent(
                state = state,
                onTimeFilterChanged = viewModel::onTimeFilterChanged,
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineStatsContent(
    state: RoutineStatsState,
    onTimeFilterChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimensions.screenPaddingHorizontal),
        verticalArrangement = Arrangement.spacedBy(Dimensions.cardSpacing),
    ) {
        item(key = "top_spacer") {
            Spacer(modifier = Modifier.height(Dimensions.elementSpacing))
        }

        item(key = "time_filter") {
            val options = listOf("Week", "Month", "Year")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = index == state.timeFilter,
                        onClick = { onTimeFilterChanged(index) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    ) {
                        Text(text = label)
                    }
                }
            }
        }

        item(key = "summary_cards") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.cardSpacing),
            ) {
                SummaryStatCard(
                    title = "Total Routines",
                    value = state.totalRoutines.toString(),
                    modifier = Modifier.weight(1f),
                )
                SummaryStatCard(
                    title = "Completed Today",
                    value = state.completedToday.toString(),
                    modifier = Modifier.weight(1f),
                )
                SummaryStatCard(
                    title = "Completion Rate",
                    value = "${(state.overallCompletionRate * 100).toInt()}%",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(key = "activity_chart") {
            ActivityChartCard(
                data = state.activityData,
                timeFilter = state.timeFilter,
            )
        }

        item(key = "routine_stats_header") {
            Text(
                text = "Per-routine stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = Dimensions.sectionSpacing),
            )
        }

        if (state.routineStats.isEmpty()) {
            item(key = "empty_routine_stats") {
                Text(
                    text = "No routines tracked yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(
                items = state.routineStats,
                key = { item -> item.routineId },
            ) { routineStat ->
                RoutineStatCard(routineStat = routineStat)
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(Dimensions.sectionSpacing))
        }
    }
}

@Composable
private fun SummaryStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimensions.elementSpacingSmall),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ActivityChartCard(
    data: List<RoutineActivityPoint>,
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
                .padding(Dimensions.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimensions.elementSpacing),
        ) {
            Text(
                text = "Daily completions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (data.isEmpty()) {
                Text(
                    text = "No activity data yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val axisLabels =
                    if (data.size <= 7) {
                        data.map { it.label }
                    } else {
                        listOf(data.first().label, data[data.lastIndex / 2].label, data.last().label)
                    }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val scrollState = rememberScrollState()
                    val minPointWidth =
                        when (timeFilter) {
                            2 -> Dimensions.elementSpacing
                            1 -> Dimensions.elementSpacingLarge
                            else -> Dimensions.sectionSpacing
                        }
                    val desiredWidth = (data.size * minPointWidth.value).dp
                    val chartWidth = if (desiredWidth > maxWidth) desiredWidth else maxWidth

                    Column(
                        modifier = Modifier
                            .horizontalScroll(scrollState)
                            .width(chartWidth),
                        verticalArrangement = Arrangement.spacedBy(Dimensions.elementSpacingSmall),
                    ) {
                        RoutineActivityBarChart(
                            data = data,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            axisLabels.forEach { label ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineActivityBarChart(
    data: List<RoutineActivityPoint>,
    modifier: Modifier = Modifier,
) {
    val maxValue = data.maxOfOrNull { point -> maxOf(point.totalCount, point.completedCount) }?.coerceAtLeast(1) ?: 1
    val completedBarColor = MaterialTheme.colorScheme.primary
    val totalBarColor = completedBarColor.copy(alpha = 0.25f)

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val pointWidth = size.width / data.size
        val barWidth = pointWidth * 0.6f

        data.forEachIndexed { index, point ->
            val left = index * pointWidth + (pointWidth - barWidth) / 2f

            val totalHeight = (point.totalCount.toFloat() / maxValue.toFloat()) * size.height
            val totalTop = size.height - totalHeight
            drawRect(
                color = totalBarColor,
                topLeft = Offset(left, totalTop),
                size = Size(width = barWidth, height = totalHeight),
            )

            val completedHeight = (point.completedCount.toFloat() / maxValue.toFloat()) * size.height
            val completedTop = size.height - completedHeight
            drawRect(
                color = completedBarColor,
                topLeft = Offset(left, completedTop),
                size = Size(width = barWidth, height = completedHeight),
            )
        }
    }
}

@Composable
private fun RoutineStatCard(
    routineStat: RoutineStatItem,
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
                .padding(Dimensions.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimensions.elementSpacing),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = routineStat.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${(routineStat.completionRate * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Text(
                text = "${routineStat.completionCount} completed / ${routineStat.totalScheduledDays} scheduled days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.sectionSpacing),
            ) {
                Text(
                    text = "Current streak: ${routineStat.currentStreak}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Longest: ${routineStat.longestStreak}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
