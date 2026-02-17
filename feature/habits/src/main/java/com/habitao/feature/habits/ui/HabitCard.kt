package com.habitao.feature.habits.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    onComplete: () -> Unit,
    onIncrement: () -> Unit,
    onTap: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = log?.isCompleted == true
    val progress = log?.progress ?: 0f
    val currentValue = log?.currentValue ?: 0
    val targetValue = log?.targetValue ?: habit.targetValue

    var hasTriggeredAction by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (!hasTriggeredAction && !isCompleted) {
                        hasTriggeredAction = true
                        onComplete()
                    }
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (!hasTriggeredAction) {
                        hasTriggeredAction = true
                        onDelete()
                    }
                    true
                }
                SwipeToDismissBoxValue.Settled -> {
                    hasTriggeredAction = false
                    false
                }
            }
        }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.Settled) {
            hasTriggeredAction = false
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            SwipeBackground(
                dismissValue = dismissState.targetValue,
                progress = dismissState.progress
            )
        },
        enableDismissFromStartToEnd = !isCompleted,
        enableDismissFromEndToStart = true
    ) {
        HabitCardContent(
            habit = habit,
            log = log,
            isCompleted = isCompleted,
            progress = progress,
            currentValue = currentValue,
            targetValue = targetValue,
            streakCount = streakCount,
            onComplete = onComplete,
            onIncrement = onIncrement,
            onTap = onTap
        )
    }
}

@Composable
private fun SwipeBackground(
    dismissValue: SwipeToDismissBoxValue,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (dismissValue) {
            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
            SwipeToDismissBoxValue.Settled -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "swipe_background_color"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (progress > 0.1f) 1f else 0.5f,
        animationSpec = tween(durationMillis = 150),
        label = "swipe_icon_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(horizontal = 24.dp),
        contentAlignment = when (dismissValue) {
            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
            SwipeToDismissBoxValue.Settled -> Alignment.Center
        }
    ) {
        when (dismissValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Complete habit",
                    modifier = Modifier
                        .scale(iconScale)
                        .size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            SwipeToDismissBoxValue.EndToStart -> {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete habit",
                    modifier = Modifier
                        .scale(iconScale)
                        .size(28.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
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
    onComplete: () -> Unit,
    onIncrement: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardContainerColor by animateColorAsState(
        targetValue = if (isCompleted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(durationMillis = 300),
        label = "card_color"
    )

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = cardContainerColor
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isCompleted) 1.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val description = habit.description
                    if (!description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                HabitActionButton(
                    habit = habit,
                    isCompleted = isCompleted,
                    onComplete = onComplete,
                    onIncrement = onIncrement
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (habit.habitType) {
                HabitType.SIMPLE -> {
                    SimpleHabitStatus(
                        isCompleted = isCompleted,
                        frequency = habit.getFrequencyDescription(),
                        streakCount = streakCount
                    )
                }
                HabitType.MEASURABLE -> {
                    MeasurableHabitProgress(
                        currentValue = currentValue,
                        targetValue = targetValue,
                        unit = habit.unit ?: "times",
                        progress = progress,
                        streakCount = streakCount
                    )
                }
                HabitType.CHECKLIST -> {
                    ChecklistHabitProgress(
                        completedItems = log?.completedChecklistItems?.size ?: 0,
                        totalItems = habit.checklist.size,
                        progress = progress,
                        streakCount = streakCount
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
    onComplete: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (habit.habitType) {
        HabitType.SIMPLE -> {
            FilledIconToggleButton(
                checked = isCompleted,
                onCheckedChange = { if (!isCompleted) onComplete() },
                modifier = modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconToggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = if (isCompleted) "Completed" else "Mark complete",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        HabitType.MEASURABLE, HabitType.CHECKLIST -> {
            FilledIconToggleButton(
                checked = isCompleted,
                onCheckedChange = { onIncrement() },
                modifier = modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconToggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = if (isCompleted) "Completed" else "Add progress",
                    modifier = Modifier.size(20.dp)
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isCompleted) "Done" else frequency,
            style = MaterialTheme.typography.labelMedium,
            color = if (isCompleted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (isCompleted) FontWeight.Medium else FontWeight.Normal
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$currentValue / $targetValue $unit",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (streakCount > 0) {
                StreakBadge(streakCount = streakCount)
            }
        }
    }
}

@Composable
private fun ChecklistHabitProgress(
    completedItems: Int,
    totalItems: Int,
    progress: Float,
    streakCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$completedItems / $totalItems items",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (streakCount > 0) {
                StreakBadge(streakCount = streakCount)
            }
        }
    }
}

@Composable
private fun StreakBadge(
    streakCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = CircleShape
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.LocalFireDepartment,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Text(
            text = streakCount.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.SemiBold
        )
    }
}
