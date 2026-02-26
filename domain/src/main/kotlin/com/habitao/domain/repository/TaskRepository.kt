package com.habitao.domain.repository

import com.habitao.domain.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    suspend fun createTask(task: Task): Result<Unit>
    suspend fun updateTask(task: Task): Result<Unit>
    suspend fun deleteTask(taskId: String): Result<Unit>
    suspend fun getTaskById(taskId: String): Result<Task>
    fun observeTaskById(taskId: String): Flow<Result<Task>>
    fun observeAllTasks(): Flow<Result<List<Task>>>
    suspend fun getAllTasks(): Result<List<Task>>
    suspend fun getTasksForDate(date: LocalDate): Result<List<Task>>
    fun observeTasksForDate(date: LocalDate): Flow<Result<List<Task>>>
    fun observeTasksForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Result<List<Task>>>
    suspend fun getSubtasksByParentId(parentId: String): Result<List<Task>>
    suspend fun deleteSubtasksByParentId(parentId: String): Result<Unit>
    suspend fun getCompletedTaskCountInRange(startMillis: Long, endMillis: Long): Result<Int>
    suspend fun getTotalTopLevelTaskCount(): Result<Int>
}
