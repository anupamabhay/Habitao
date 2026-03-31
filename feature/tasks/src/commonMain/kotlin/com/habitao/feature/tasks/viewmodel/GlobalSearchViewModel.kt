package com.habitao.feature.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitao.domain.repository.HabitRepository
import com.habitao.domain.repository.RoutineRepository
import com.habitao.domain.repository.TaskRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class SearchResultType {
    TASK,
    HABIT,
    ROUTINE,
}

enum class SearchFilter {
    ALL,
    TASKS,
    HABITS,
    ROUTINES,
}

data class SearchResultItem(
    val id: String,
    val title: String,
    val description: String?,
    val type: SearchResultType,
)

data class GlobalSearchState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.ALL,
    val results: List<SearchResultItem> = emptyList(),
    val isLoading: Boolean = true,
)

@OptIn(FlowPreview::class)
class GlobalSearchViewModel(
    taskRepository: TaskRepository,
    habitRepository: HabitRepository,
    routineRepository: RoutineRepository,
) : ViewModel() {
    private val queryFlow = MutableStateFlow("")
    private val filterFlow = MutableStateFlow(SearchFilter.ALL)

    val state: StateFlow<GlobalSearchState> =
        combine(
            queryFlow.debounce(250),
            filterFlow,
            taskRepository.observeAllTasks().map { it.getOrElse { emptyList() } },
            habitRepository.observeAllHabits().map { it.getOrElse { emptyList() } },
            routineRepository.observeAllRoutines().map { it.getOrElse { emptyList() } },
        ) { query, filter, tasks, habits, routines ->
            val normalized = query.trim().lowercase()
            val allResults =
                if (normalized.isBlank()) {
                    emptyList()
                } else {
                    buildList {
                        fun addMatches(
                            items: List<SearchResultItem>,
                            type: SearchResultType,
                        ) {
                            items.filter { item ->
                                item.title.lowercase().contains(normalized) ||
                                    item.description?.lowercase()?.contains(normalized) == true
                            }.forEach { item ->
                                add(item.copy(type = type))
                            }
                        }

                        addMatches(
                            tasks.map { task ->
                                SearchResultItem(
                                    id = task.id,
                                    title = task.title,
                                    description = task.description,
                                    type = SearchResultType.TASK,
                                )
                            },
                            SearchResultType.TASK,
                        )
                        addMatches(
                            habits.map { habit ->
                                SearchResultItem(
                                    id = habit.id,
                                    title = habit.title,
                                    description = habit.description,
                                    type = SearchResultType.HABIT,
                                )
                            },
                            SearchResultType.HABIT,
                        )
                        addMatches(
                            routines.map { routine ->
                                SearchResultItem(
                                    id = routine.id,
                                    title = routine.title,
                                    description = routine.description,
                                    type = SearchResultType.ROUTINE,
                                )
                            },
                            SearchResultType.ROUTINE,
                        )
                    }
                }

            val filteredResults =
                when (filter) {
                    SearchFilter.ALL -> allResults
                    SearchFilter.TASKS -> allResults.filter { it.type == SearchResultType.TASK }
                    SearchFilter.HABITS -> allResults.filter { it.type == SearchResultType.HABIT }
                    SearchFilter.ROUTINES -> allResults.filter { it.type == SearchResultType.ROUTINE }
                }

            GlobalSearchState(
                query = query,
                filter = filter,
                results = filteredResults.sortedBy { it.title.lowercase() },
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GlobalSearchState(),
        )

    fun setQuery(query: String) {
        queryFlow.value = query
    }

    fun setFilter(filter: SearchFilter) {
        filterFlow.value = filter
    }
}
