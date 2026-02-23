package com.habitao.feature.routines.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habitao.core.ui.theme.AppShapes
import com.habitao.core.ui.theme.Dimensions
import com.habitao.domain.model.Routine
import com.habitao.domain.model.RoutineLog
import com.habitao.domain.model.RoutineStep
import com.habitao.feature.routines.viewmodel.RoutinesIntent
import com.habitao.feature.routines.viewmodel.RoutinesState
import com.habitao.feature.routines.viewmodel.RoutinesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    onAddRoutine: () -> Unit,
    viewModel: RoutinesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.processIntent(RoutinesIntent.ClearError)
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Routines") },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddRoutine,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(Dimensions.elementSpacing))
                Text("New Routine")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        RoutinesContent(
            state = state,
            onToggleStep = { routineId, stepId, isCompleted ->
                viewModel.processIntent(RoutinesIntent.ToggleStep(routineId, stepId, isCompleted))
            },
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@Composable
private fun RoutinesContent(
    state: RoutinesState,
    onToggleStep: (String, String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            state.routines.isEmpty() -> {
                EmptyState(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Dimensions.screenPaddingHorizontal,
                        top = Dimensions.elementSpacing,
                        end = Dimensions.screenPaddingHorizontal,
                        bottom = Dimensions.fabClearance,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.cardSpacing),
                ) {
                    items(
                        items = state.routines,
                        key = { it.id },
                    ) { routine ->
                        RoutineCard(
                            routine = routine,
                            steps = state.steps[routine.id] ?: emptyList(),
                            log = state.logs[routine.id],
                            onToggleStep = { stepId, isCompleted ->
                                onToggleStep(routine.id, stepId, isCompleted)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineCard(
    routine: Routine,
    steps: List<RoutineStep>,
    log: RoutineLog?,
    onToggleStep: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    
    val completedStepsCount = log?.completedStepIds?.size ?: 0
    val totalStepsCount = steps.size
    val progress = if (totalStepsCount > 0) {
        completedStepsCount.toFloat() / totalStepsCount
    } else {
        0f
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.cardPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = routine.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(Dimensions.elementSpacingSmall))
                    Text(
                        text = "$completedStepsCount of $totalStepsCount steps completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                    )
                }
            }

            if (totalStepsCount > 0) {
                Spacer(modifier = Modifier.height(Dimensions.elementSpacing))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimensions.progressBarHeight)
                        .clip(AppShapes.progressBar),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimensions.elementSpacing),
                ) {
                    steps.sortedBy { it.stepOrder }.forEach { step ->
                        val isCompleted = log?.completedStepIds?.contains(step.id) == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleStep(step.id, !isCompleted) }
                                .padding(vertical = Dimensions.elementSpacingSmall),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = isCompleted,
                                onCheckedChange = { onToggleStep(step.id, it) },
                            )
                            Spacer(modifier = Modifier.width(Dimensions.elementSpacing))
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCompleted) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ListAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        )

        Spacer(modifier = Modifier.height(Dimensions.sectionSpacing))

        Text(
            text = "No routines yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
