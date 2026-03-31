package com.habitao.feature.tasks.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.habitao.core.ui.theme.Dimensions
import com.habitao.feature.tasks.viewmodel.GlobalSearchViewModel
import com.habitao.feature.tasks.viewmodel.SearchFilter
import com.habitao.feature.tasks.viewmodel.SearchResultItem
import com.habitao.feature.tasks.viewmodel.SearchResultType
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    onNavigateBack: () -> Unit,
    onOpenTask: (String) -> Unit,
    onOpenHabit: (String) -> Unit,
    onOpenRoutine: (String) -> Unit,
    viewModel: GlobalSearchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Global Search") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Dimensions.screenPaddingHorizontal),
            verticalArrangement = Arrangement.spacedBy(Dimensions.elementSpacing),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search tasks, habits, routines") },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.elementSpacing)) {
                FilterChip("All", state.filter == SearchFilter.ALL) { viewModel.setFilter(SearchFilter.ALL) }
                FilterChip("Tasks", state.filter == SearchFilter.TASKS) { viewModel.setFilter(SearchFilter.TASKS) }
                FilterChip("Habits", state.filter == SearchFilter.HABITS) { viewModel.setFilter(SearchFilter.HABITS) }
                FilterChip("Routines", state.filter == SearchFilter.ROUTINES) { viewModel.setFilter(SearchFilter.ROUTINES) }
            }

            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                state.query.isBlank() -> {
                    Text("Type to search across tasks, habits, and routines.")
                }
                state.results.isEmpty() -> {
                    Text("No results found.")
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = Dimensions.fabClearance),
                        verticalArrangement = Arrangement.spacedBy(Dimensions.elementSpacing),
                    ) {
                        items(state.results, key = { "${it.type}-${it.id}" }) { result ->
                            SearchResultRow(
                                result = result,
                                onOpenTask = onOpenTask,
                                onOpenHabit = onOpenHabit,
                                onOpenRoutine = onOpenRoutine,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { if (selected) Icon(Icons.Default.Check, contentDescription = null) },
    )
}

@Composable
private fun SearchResultRow(
    result: SearchResultItem,
    onOpenTask: (String) -> Unit,
    onOpenHabit: (String) -> Unit,
    onOpenRoutine: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    when (result.type) {
                        SearchResultType.TASK -> onOpenTask(result.id)
                        SearchResultType.HABIT -> onOpenHabit(result.id)
                        SearchResultType.ROUTINE -> onOpenRoutine(result.id)
                    }
                }
                .padding(vertical = Dimensions.elementSpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = when (result.type) {
                SearchResultType.TASK -> "Task"
                SearchResultType.HABIT -> "Habit"
                SearchResultType.ROUTINE -> "Routine"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(Dimensions.elementSpacingSmall))
        Column {
            Text(result.title, style = MaterialTheme.typography.bodyLarge)
            result.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
