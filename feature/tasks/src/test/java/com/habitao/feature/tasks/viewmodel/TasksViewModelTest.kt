package com.habitao.feature.tasks.viewmodel

import com.habitao.domain.model.Task
import com.habitao.domain.model.TaskPriority
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
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeTaskRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeTaskRepository()
    }

    @Test
    fun `collapsed parent task state should survive state refreshes`() =
        runTest {
            val parent = createTask(id = "parent-1", title = "Parent task", dueDate = LocalDate(2099, 1, 1))
            val child = createTask(id = "child-1", title = "Child task", parentTaskId = parent.id)
            repository.emitTasks(listOf(parent, child))

            val viewModel = TasksViewModel(repository)
            advanceUntilIdle()

            viewModel.processIntent(TasksIntent.SetTaskExpanded(taskId = parent.id, isExpanded = false))
            advanceUntilIdle()

            assertFalse(viewModel.state.value.expandedTaskIds[parent.id] ?: true)

            repository.emitTasks(listOf(parent, child, createTask(id = "task-2", title = "Another task", dueDate = LocalDate(2099, 1, 2))))
            advanceUntilIdle()

            val updatedState = viewModel.state.value
            assertFalse(updatedState.expandedTaskIds[parent.id] ?: true)
            assertEquals(1, updatedState.subTasks[parent.id]?.size)
        }

    @Test
    fun `tasks without due dates should be routed to inbox instead of upcoming`() =
        runTest {
            repository.emitTasks(
                listOf(
                    createTask(id = "inbox-1", title = "Inbox task"),
                    createTask(id = "upcoming-1", title = "Upcoming task", dueDate = LocalDate(2099, 1, 2)),
                ),
            )

            val viewModel = TasksViewModel(repository)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(listOf("inbox-1"), state.inboxTasks.map { it.id })
            assertEquals(listOf("upcoming-1"), state.upcomingTasks.map { it.id })
        }

    private fun createTask(
        id: String,
        title: String,
        parentTaskId: String? = null,
        dueDate: LocalDate? = null,
    ): Task =
        Task(
            id = id,
            title = title,
            parentTaskId = parentTaskId,
            dueDate = dueDate,
            priority = TaskPriority.NONE,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        )
}

private class FakeTaskRepository : TaskRepository {
    private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())

    fun emitTasks(tasks: List<Task>) {
        tasksFlow.value = tasks
    }

    override fun observeAllTasks(): Flow<Result<List<Task>>> = tasksFlow.map { Result.success(it) }

    override suspend fun createTask(task: Task): Result<Unit> = Result.success(Unit)

    override suspend fun updateTask(task: Task): Result<Unit> = Result.success(Unit)

    override suspend fun upsertTask(task: Task): Result<Unit> = Result.success(Unit)

    override suspend fun deleteTask(taskId: String): Result<Unit> = Result.success(Unit)

    override suspend fun getTaskById(taskId: String): Result<Task> =
        tasksFlow.value.firstOrNull { it.id == taskId }?.let { Result.success(it) }
            ?: Result.failure(IllegalArgumentException("Task not found"))

    override fun observeTaskById(taskId: String): Flow<Result<Task>> =
        tasksFlow.map { tasks ->
            tasks.firstOrNull { it.id == taskId }?.let { Result.success(it) }
                ?: Result.failure(IllegalArgumentException("Task not found"))
        }

    override suspend fun getAllTasks(): Result<List<Task>> = Result.success(tasksFlow.value)

    override suspend fun getTasksForDate(date: LocalDate): Result<List<Task>> =
        Result.success(tasksFlow.value.filter { it.dueDate == date })

    override fun observeTasksForDate(date: LocalDate): Flow<Result<List<Task>>> =
        tasksFlow.map { tasks -> Result.success(tasks.filter { it.dueDate == date }) }

    override fun observeTasksForDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<Result<List<Task>>> =
        tasksFlow.map { tasks ->
            Result.success(
                tasks.filter { task ->
                    task.dueDate?.let { dueDate -> dueDate >= startDate && dueDate <= endDate } ?: false
                },
            )
        }

    override suspend fun getSubtasksByParentId(parentId: String): Result<List<Task>> =
        Result.success(tasksFlow.value.filter { it.parentTaskId == parentId })

    override suspend fun deleteSubtasksByParentId(parentId: String): Result<Unit> = Result.success(Unit)

    override suspend fun getCompletedTaskCountInRange(
        startMillis: Long,
        endMillis: Long,
    ): Result<Int> = Result.success(0)

    override suspend fun getTotalTopLevelTaskCount(): Result<Int> =
        Result.success(tasksFlow.value.count { it.parentTaskId == null })
}
