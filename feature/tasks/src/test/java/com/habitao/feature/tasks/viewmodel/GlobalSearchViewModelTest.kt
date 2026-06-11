package com.habitao.feature.tasks.viewmodel

import com.habitao.domain.model.DayOfWeek
import com.habitao.domain.model.Habit
import com.habitao.domain.model.HabitLog
import com.habitao.domain.model.RepeatPattern
import com.habitao.domain.model.Routine
import com.habitao.domain.model.RoutineLog
import com.habitao.domain.model.RoutineStep
import com.habitao.domain.model.StreakInfo
import com.habitao.domain.model.Task
import com.habitao.domain.repository.HabitRepository
import com.habitao.domain.repository.RoutineRepository
import com.habitao.domain.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalSearchViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun `search should aggregate tasks habits and routines`() =
        runTest {
            val taskRepo = FakeSearchTaskRepository()
            val habitRepo = FakeHabitRepository()
            val routineRepo = FakeRoutineRepository()

            taskRepo.emit(listOf(Task(id = "t1", title = "Plan sprint")))
            habitRepo.emit(listOf(Habit(id = "h1", title = "Sprint workout")))
            routineRepo.emit(listOf(createRoutine(id = "r1", title = "Sprint kickoff")))

            val viewModel = GlobalSearchViewModel(taskRepo, habitRepo, routineRepo)
            viewModel.setQuery("sprint")
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(3, state.results.size)
            assertTrue(state.results.any { it.type == SearchResultType.TASK })
            assertTrue(state.results.any { it.type == SearchResultType.HABIT })
            assertTrue(state.results.any { it.type == SearchResultType.ROUTINE })
        }

    @Test
    fun `search filter should limit result type`() =
        runTest {
            val taskRepo = FakeSearchTaskRepository()
            val habitRepo = FakeHabitRepository()
            val routineRepo = FakeRoutineRepository()

            taskRepo.emit(listOf(Task(id = "t1", title = "Read docs")))
            habitRepo.emit(listOf(Habit(id = "h1", title = "Read every day")))
            routineRepo.emit(listOf(createRoutine(id = "r1", title = "Morning read routine")))

            val viewModel = GlobalSearchViewModel(taskRepo, habitRepo, routineRepo)
            viewModel.setQuery("read")
            viewModel.setFilter(SearchFilter.TASKS)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(1, state.results.size)
            assertEquals(SearchResultType.TASK, state.results.first().type)
        }

    private fun createRoutine(id: String, title: String): Routine =
        Routine(
            id = id,
            title = title,
            repeatPattern = RepeatPattern.DAILY,
            repeatDays = listOf(DayOfWeek.MONDAY),
            startDate = LocalDate(2026, 1, 1),
            nextScheduledDate = LocalDate(2026, 1, 1),
        )
}

private class FakeSearchTaskRepository : TaskRepository {
    private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())

    fun emit(tasks: List<Task>) {
        tasksFlow.value = tasks
    }

    override fun observeAllTasks(): Flow<Result<List<Task>>> = tasksFlow.map { Result.success(it) }
    override suspend fun createTask(task: Task): Result<Unit> = Result.success(Unit)
    override suspend fun updateTask(task: Task): Result<Unit> = Result.success(Unit)
    override suspend fun upsertTask(task: Task): Result<Unit> = Result.success(Unit)
    override suspend fun deleteTask(taskId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getTaskById(taskId: String): Result<Task> = Result.failure(Exception())
    override fun observeTaskById(taskId: String): Flow<Result<Task>> = tasksFlow.map { Result.failure(Exception()) }
    override suspend fun getAllTasks(): Result<List<Task>> = Result.success(tasksFlow.value)
    override suspend fun getTasksForDate(date: LocalDate): Result<List<Task>> = Result.success(emptyList())
    override fun observeTasksForDate(date: LocalDate): Flow<Result<List<Task>>> = tasksFlow.map { Result.success(emptyList()) }
    override fun observeTasksForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Result<List<Task>>> = tasksFlow.map { Result.success(emptyList()) }
    override suspend fun getSubtasksByParentId(parentId: String): Result<List<Task>> = Result.success(emptyList())
    override suspend fun deleteSubtasksByParentId(parentId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getCompletedTaskCountInRange(startMillis: Long, endMillis: Long): Result<Int> = Result.success(0)
    override suspend fun getTotalTopLevelTaskCount(): Result<Int> = Result.success(0)
}

private class FakeHabitRepository : HabitRepository {
    private val habitsFlow = MutableStateFlow<List<Habit>>(emptyList())

    fun emit(habits: List<Habit>) {
        habitsFlow.value = habits
    }

    override fun observeAllHabits(): Flow<Result<List<Habit>>> = habitsFlow.map { Result.success(it) }
    override suspend fun createHabit(habit: Habit): Result<Unit> = Result.success(Unit)
    override suspend fun updateHabit(habit: Habit): Result<Unit> = Result.success(Unit)
    override suspend fun deleteHabit(habitId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getHabitById(habitId: String): Result<Habit> = Result.failure(Exception())
    override fun observeHabitById(habitId: String): Flow<Result<Habit>> = habitsFlow.map { Result.failure(Exception()) }
    override suspend fun getAllHabits(): Result<List<Habit>> = Result.success(habitsFlow.value)
    override fun observeArchivedHabits(): Flow<Result<List<Habit>>> = habitsFlow.map { Result.success(emptyList()) }
    override suspend fun getHabitsForDate(date: LocalDate): Result<List<Habit>> = Result.success(emptyList())
    override fun observeHabitsForDate(date: LocalDate): Flow<Result<List<Habit>>> = habitsFlow.map { Result.success(emptyList()) }
    override suspend fun createOrUpdateLog(habitId: String, date: LocalDate, count: Int): Result<Unit> = Result.success(Unit)
    override suspend fun getLogForHabitAndDate(habitId: String, date: LocalDate): Result<HabitLog?> = Result.success(null)
    override fun observeLogForHabitAndDate(habitId: String, date: LocalDate): Flow<Result<HabitLog?>> = habitsFlow.map { Result.success(null) }
    override suspend fun getLogsForHabit(habitId: String): Result<List<HabitLog>> = Result.success(emptyList())
    override fun observeLogsForDate(date: LocalDate): Flow<Result<Map<String, HabitLog>>> = habitsFlow.map { Result.success(emptyMap()) }
    override fun observeLogsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Result<List<HabitLog>>> = habitsFlow.map { Result.success(emptyList()) }
    override suspend fun getWeeklyProgressForHabit(habitId: String, weekContainingDate: LocalDate): Result<Int> = Result.success(0)
    override suspend fun calculateStreak(habitId: String): Result<StreakInfo> = Result.success(StreakInfo(0, 0, 0))
    override suspend fun archiveHabit(habitId: String, archive: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun toggleChecklistItem(habitId: String, date: LocalDate, itemId: String): Result<Unit> = Result.success(Unit)
}

private class FakeRoutineRepository : RoutineRepository {
    private val routinesFlow = MutableStateFlow<List<Routine>>(emptyList())

    fun emit(routines: List<Routine>) {
        routinesFlow.value = routines
    }

    override fun observeAllRoutines(): Flow<Result<List<Routine>>> = routinesFlow.map { Result.success(it) }
    override suspend fun createRoutine(routine: Routine, steps: List<RoutineStep>): Result<Unit> = Result.success(Unit)
    override suspend fun updateRoutine(routine: Routine, steps: List<RoutineStep>): Result<Unit> = Result.success(Unit)
    override suspend fun upsertRoutine(routine: Routine, steps: List<RoutineStep>): Result<Unit> = Result.success(Unit)
    override suspend fun deleteRoutine(routineId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getRoutineById(routineId: String): Result<Routine> = Result.failure(Exception())
    override fun observeRoutineById(routineId: String): Flow<Result<Routine>> = routinesFlow.map { Result.failure(Exception()) }
    override suspend fun getRoutinesForDate(date: LocalDate): Result<List<Routine>> = Result.success(emptyList())
    override fun observeRoutinesForDate(date: LocalDate): Flow<Result<List<Routine>>> = routinesFlow.map { Result.success(emptyList()) }
    override suspend fun getRoutineSteps(routineId: String): Result<List<RoutineStep>> = Result.success(emptyList())
    override fun observeRoutineSteps(routineId: String): Flow<Result<List<RoutineStep>>> = routinesFlow.map { Result.success(emptyList()) }
    override suspend fun logRoutineStep(routineId: String, stepId: String, date: LocalDate, isCompleted: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun getRoutineLog(routineId: String, date: LocalDate): Result<RoutineLog?> = Result.success(null)
    override fun observeRoutineLog(routineId: String, date: LocalDate): Flow<Result<RoutineLog?>> = routinesFlow.map { Result.success(null) }
    override fun observeRoutineLogsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Result<List<RoutineLog>>> = routinesFlow.map { Result.success(emptyList()) }
    override suspend fun getCompletedRoutinesCount(date: LocalDate): Result<Int> = Result.success(0)
    override suspend fun getTotalRoutinesCount(): Result<Int> = Result.success(0)
}

