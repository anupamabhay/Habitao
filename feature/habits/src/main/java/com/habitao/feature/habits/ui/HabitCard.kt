package com.habitao.feature.habits.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.habitao.core.ui.theme.AppShapes
import com.habitao.core.ui.theme.Dimensions
import com.habitao.domain.model.ChecklistItem
import com.habitao.domain.model.FrequencyType
import com.habitao.domain.model.Habit
import com.habitao.domain.model.HabitLog
import com.habitao.domain.model.HabitType

/**
 * A polished habit card component following Material Design 3 patterns.
 * Supports swipe-to-complete gesture, progress visualization, and all habit types.
 *
 * @param habit The habit to display
 * @param log The current day's log for this habit (null if no progress recorded)
 * @param streakCount Current streak count for the habit
 * @param onComplete Called when habit is completed via swipe or toggle
 * @param onIncrement Called when incrementing progress for MEASURABLE habits
 * @param onTap Called when the card is tapped for navigation to details/edit
 * @param onDelete Called when the habit should be deleted (swipe left)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitCard(
    habit: Habit,
    log: HabitLog?,
    streakCount: Int,
    weeklyProgress: Int? = null,
    onComplete: () -> Unit,
    onUncomplete: () -> Unit,
    onIncrement: () -> Unit,
    onTap: () -> Unit,
    onDelete: () -> Unit,
    onToggleChecklistItem: (itemId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isCompleted = log?.isCompleted == true
    // For period-based habits (TIMES_PER_WEEK, EVERY_X_DAYS), use aggregated progress
    val isPeriodBased =
        habit.frequencyType == FrequencyType.TIMES_PER_WEEK ||
            habit.frequencyType == FrequencyType.EVERY_X_DAYS
    val displayValue = if (isPeriodBased && weeklyProgress != null) weeklyProgress else (log?.currentValue ?: 0)
    val targetValue = if (isPeriodBased) habit.frequencyValue else (log?.targetValue ?: habit.targetValue)
    val progress =
        if (targetValue > 0) {
            (displayValue.toFloat() / targetValue).coerceIn(
                0f,
                1f,
            )
        } else {
            (log?.progress ?: 0f)
        }
    val currentValue = log?.currentValue ?: 0

    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                when (dismissValue) {
                    SwipeToDismissBoxValue.StartToEnd -> false
                    SwipeToDismissBoxValue.EndToStart -> {
                        onDelete()
                        true
                    }
                    SwipeToDismissBoxValue.Settled -> false
                }
            },
        )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            SwipeBackground(
                dismissValue = dismissState.targetValue,
                progress = dismissState.progress,
            )
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
    ) {
        HabitCardContent(
            habit = habit,
            log = log,
            isCompleted = isCompleted,
            progress = progress,
            currentValue = currentValue,
            targetValue = targetValue,
            streakCount = streakCount,
            weeklyProgress = weeklyProgress,
            onComplete = onComplete,
            onUncomplete = onUncomplete,
            onIncrement = onIncrement,
            onTap = onTap,
            onToggleChecklistItem = onToggleChecklistItem,
        )
    }
}

@Composable
private fun SwipeBackground(
    dismissValue: SwipeToDismissBoxValue,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue =
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> Color.Transparent
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            },
        animationSpec = tween(durationMillis = 200),
        label = "swipe_background_color",
    )

    val iconScale by animateFloatAsState(
        targetValue = if (progress > 0.1f) 1f else 0.5f,
        animationSpec = tween(durationMillis = 150),
        label = "swipe_icon_scale",
    )

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.large)
                .background(backgroundColor)
                .padding(horizontal = Dimensions.sectionSpacing),
        contentAlignment =
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.Settled -> Alignment.Center
            },
    ) {
        when (dismissValue) {
            SwipeToDismissBoxValue.StartToEnd -> { }
            SwipeToDismissBoxValue.EndToStart -> {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete habit",
                    modifier =
                        Modifier
                            .scale(iconScale)
                            .size(28.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            SwipeToDismissBoxValue.Settled -> { }
        }
    }
}

@Composable
private fun HabitCardContent(
    habit: Habit,
    log: HabitLog?,
    isCompleted: Boolean,
    progress: Float,
    currentValue: Int,
    targetValue: Int,
    streakCount: Int,
    weeklyProgress: Int?,
    onComplete: () -> Unit,
    onUncomplete: () -> Unit,
    onIncrement: () -> Unit,
    onTap: () -> Unit,
    onToggleChecklistItem: (itemId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    val cardContainerColor by animateColorAsState(
        targetValue =
            if (isCompleted) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        animationSpec = tween(durationMillis = 300),
        label = "card_color",
    )

    ElevatedCard(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onTap),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = cardContainerColor,
            ),
        elevation =
            CardDefaults.elevatedCardElevation(
                defaultElevation =
                    if (isCompleted) {
                        Dimensions.cardElevationCompleted
                    } else {
                        Dimensions.cardElevation
                    },
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.cardPadding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color =
                            if (isCompleted) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    )

                    val description = habit.description
                    if (!description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = if (isCompleted) 0.5f else 1f,
                                ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(Dimensions.elementSpacingLarge))

                HabitActionButton(
                    habit = habit,
                    isCompleted = isCompleted,
                    currentValue = currentValue,
                    isExpanded = isExpanded,
                    onComplete = onComplete,
                    onUncomplete = onUncomplete,
                    onIncrement = onIncrement,
                    onToggleExpand = { isExpanded = !isExpanded },
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.elementSpacingLarge))

            when (habit.habitType) {
                HabitType.SIMPLE -> {
                    SimpleHabitStatus(
                        isCompleted = isCompleted,
                        frequency = habit.getFrequencyDescription(),
                        streakCount = streakCount,
                    )
                }
                HabitType.MEASURABLE -> {
                    val isPeriodBased =
                        habit.frequencyType == FrequencyType.TIMES_PER_WEEK ||
                            habit.frequencyType == FrequencyType.EVERY_X_DAYS
                    val displayCurrentValue =
                        if (isPeriodBased && weeklyProgress != null) {
                            weeklyProgress
                        } else {
                            currentValue
                        }
                    val displayTargetValue = if (isPeriodBased) habit.frequencyValue else targetValue
                    val displayUnit =
                        when {
                            habit.frequencyType == FrequencyType.TIMES_PER_WEEK -> "this week"
                            habit.frequencyType == FrequencyType.EVERY_X_DAYS -> "this cycle"
                            else -> habit.unit ?: "times"
                        }
                    val displayProgress =
                        if (displayTargetValue > 0) {
                            (displayCurrentValue.toFloat() / displayTargetValue).coerceIn(0f, 1f)
                        } else {
                            progress
                        }

                    MeasurableHabitProgress(
                        currentValue = displayCurrentValue,
                        targetValue = displayTargetValue,
                        unit = displayUnit,
                        progress = displayProgress,
                        streakCount = streakCount,
                    )
                }
                HabitType.CHECKLIST -> {
                    ChecklistHabitProgress(
                        checklist = habit.checklist,
                        completedItemIds = log?.completedChecklistItems ?: emptySet(),
                        progress = progress,
                        streakCount = streakCount,
                        isExpanded = isExpanded,
                        onToggleExpand = { isExpanded = !isExpanded },
                        onToggleItem = onToggleChecklistItem,
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitActionButton(
    habit: Habit,
    isCompleted: Boolean,
    currentValue: Int,
    isExpanded: Boolean,
    onComplete: () -> Unit,
    onUncomplete: () -> Unit,
    onIncrement: () -> Unit,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (habit.habitType) {
        HabitType.SIMPLE -> {
            FilledIconToggleButton(
                checked = isCompleted,
                onCheckedChange = { checked ->
                    if (checked) onComplete() else onUncomplete()
                },
                modifier = modifier.size(40.dp),
                colors =
                    IconButtonDefaults.filledIconToggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = if (isCompleted) "Mark incomplete" else "Mark complete",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        HabitType.MEASURABLE -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (currentValue > 0) {
                    FilledIconToggleButton(
                        checked = false,
                        onCheckedChange = { onUncomplete() },
                        modifier = Modifier.size(34.dp),
                        colors =
                            IconButtonDefaults.filledIconToggleButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                checkedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                checkedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease progress",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                FilledIconToggleButton(
                    checked = isCompleted,
                    onCheckedChange = { onIncrement() },
                    modifier = Modifier.size(40.dp),
                    colors =
                        IconButtonDefaults.filledIconToggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = if (isCompleted) "Completed" else "Add progress",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        HabitType.CHECKLIST -> {
            FilledIconToggleButton(
                checked = isExpanded,
                onCheckedChange = { onToggleExpand() },
                modifier = modifier.size(40.dp),
                colors =
                    IconButtonDefaults.filledIconToggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        checkedContainerColor = MaterialTheme.colorScheme.tertiary,
                        checkedContentColor = MaterialTheme.colorScheme.onTertiary,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse checklist" else "Expand checklist",
                    modifier =
                        Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = if (isExpanded) 180f else 0f
                            },
                )
            }
        }
    }
}

@Composable
private fun SimpleHabitStatus(
    isCompleted: Boolean,
    frequency: String,
    streakCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (isCompleted) "Done" else frequency,
            style = MaterialTheme.typography.labelMedium,
            color =
                if (isCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            fontWeight = if (isCompleted) FontWeight.Medium else FontWeight.Normal,
        )

        if (streakCount > 0) {
            StreakBadge(streakCount = streakCount)
        }
    }
}

@Composable
private fun MeasurableHabitProgress(
    currentValue: Int,
    targetValue: Int,
    unit: String,
    progress: Float,
    streakCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { progress },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(Dimensions.progressBarHeight)
                    .clip(AppShapes.progressBar),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )

        Spacer(modifier = Modifier.height(Dimensions.elementSpacing))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$currentValue / $targetValue $unit",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (streakCount > 0) {
                StreakBadge(streakCount = streakCount)
            }
        }
    }
}

@Composable
private fun ChecklistHabitProgress(
    checklist: List<ChecklistItem>,
    completedItemIds: Set<String>,
    progress: Float,
    streakCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleItem: (itemId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val completedCount = completedItemIds.size
    val totalItems = checklist.size

    val expandIconRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "expand_rotation",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { progress },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(Dimensions.progressBarHeight)
                    .clip(AppShapes.progressBar),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )

        Spacer(modifier = Modifier.height(Dimensions.elementSpacing))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onToggleExpand() }
                        .padding(
                            vertical = Dimensions.elementSpacingSmall,
                            horizontal = Dimensions.elementSpacingSmall,
                        ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "$completedCount / $totalItems items",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier =
                        Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = expandIconRotation },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (streakCount > 0) {
                StreakBadge(streakCount = streakCount)
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(durationMillis = 200)),
            exit = shrinkVertically(animationSpec = tween(durationMillis = 200)),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = Dimensions.elementSpacing),
                verticalArrangement = Arrangement.spacedBy(Dimensions.elementSpacingSmall),
            ) {
                checklist.forEach { item ->
                    val isItemCompleted = completedItemIds.contains(item.id)
                    ChecklistItemRow(
                        item = item,
                        isCompleted = isItemCompleted,
                        onToggle = { onToggleItem(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChecklistItemRow(
    item: ChecklistItem,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor by animateColorAsState(
        targetValue =
            if (isCompleted) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        animationSpec = tween(durationMillis = 150),
        label = "checklist_text_color",
    )

    val iconTint by animateColorAsState(
        targetValue =
            if (isCompleted) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.outline
            },
        animationSpec = tween(durationMillis = 150),
        label = "checklist_icon_tint",
    )

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onToggle)
                .padding(horizontal = Dimensions.elementSpacing, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.elementSpacingLarge),
    ) {
        Icon(
            imageVector =
                if (isCompleted) {
                    Icons.Default.CheckBox
                } else {
                    Icons.Default.CheckBoxOutlineBlank
                },
            contentDescription = if (isCompleted) "Completed" else "Not completed",
            modifier = Modifier.size(22.dp),
            tint = iconTint,
        )
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StreakBadge(
    streakCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .background(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = CircleShape,
                )
                .padding(
                    horizontal = Dimensions.elementSpacing,
                    vertical = Dimensions.elementSpacingSmall,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.elementSpacingSmall),
    ) {
        Icon(
            imageVector = Icons.Outlined.LocalFireDepartment,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Text(
            text = streakCount.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
