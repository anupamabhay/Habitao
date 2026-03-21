package com.habitao.data.repository

import com.habitao.data.local.dao.TaskDao
import com.habitao.domain.model.Task
import com.habitao.domain.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

class TaskRepositoryImpl
    constructor(
        private val taskDao: TaskDao,
        private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Default,
    ) : TaskRepository {
        override suspend fun createTask(task: Task): Result<Unit> =
            withContext(dispatcher) {
                try {
                    taskDao.insertTask(task.toEntity())
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override suspend fun updateTask(task: Task): Result<Unit> =
            withContext(dispatcher) {
                try {
                    taskDao.updateTask(task.toEntity())
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override suspend fun upsertTask(task: Task): Result<Unit> =
            withContext(dispatcher) {
                try {
                    taskDao.insertTask(task.toEntity())
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override suspend fun deleteTask(taskId: String): Result<Unit> =
            withContext(dispatcher) {
                try {
                    taskDao.deleteTaskById(taskId)
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override suspend fun getTaskById(taskId: String): Result<Task> =
            withContext(dispatcher) {
                try {
                    val entity =
                        taskDao.getTaskById(taskId)
                            ?: return@withContext Result.failure(Exception("Task not found"))
                    Result.success(entity.toDomainModel())
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override fun observeTaskById(taskId: String): Flow<Result<Task>> {
            return taskDao.observeTaskById(taskId)
                .map { entity ->
                    entity?.let {
                        Result.success(it.toDomainModel())
                    } ?: Result.failure(Exception("Task not found"))
                }
                .flowOn(dispatcher)
        }

        override fun observeAllTasks(): Flow<Result<List<Task>>> {
            return taskDao.observeAllTasks()
                .map { entities ->
                    Result.success(entities.map { it.toDomainModel() })
                }
                .catch { e -> emit(Result.failure(e)) }
                .flowOn(dispatcher)
        }

        override suspend fun getAllTasks(): Result<List<Task>> =
            withContext(dispatcher) {
                try {
                    val entities = taskDao.getAllTasks()
                    Result.success(entities.map { it.toDomainModel() })
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override suspend fun getTasksForDate(date: LocalDate): Result<List<Task>> =
            withContext(dispatcher) {
                try {
                    val dateMillis = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                    val entities = taskDao.getTasksForDate(dateMillis)
                    Result.success(entities.map { it.toDomainModel() })
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override fun observeTasksForDate(date: LocalDate): Flow<Result<List<Task>>> {
            val dateMillis = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            return taskDao.observeTasksForDate(dateMillis)
                .map { entities ->
                    Result.success(entities.map { it.toDomainModel() })
                }
                .catch { e -> emit(Result.failure(e)) }
                .flowOn(dispatcher)
        }

        override fun observeTasksForDateRange(
            startDate: LocalDate,
            endDate: LocalDate,
        ): Flow<Result<List<Task>>> {
            val tz = TimeZone.currentSystemDefault()
            val startMillis = startDate.atStartOfDayIn(tz).toEpochMilliseconds()
            val endMillis = endDate.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds()
            return taskDao.observeTasksForDateRange(startMillis, endMillis)
                .map { entities ->
                    Result.success(entities.map { it.toDomainModel() })
                }
                .catch { e -> emit(Result.failure(e)) }
                .flowOn(dispatcher)
        }

        override suspend fun getSubtasksByParentId(parentId: String): Result<List<Task>> =
            withContext(dispatcher) {
                try {
                    val entities = taskDao.getSubtasksByParentId(parentId)
                    Result.success(entities.map { it.toDomainModel() })
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override suspend fun deleteSubtasksByParentId(parentId: String): Result<Unit> =
            withContext(dispatcher) {
                try {
                    taskDao.deleteSubtasksByParentId(parentId)
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override suspend fun getCompletedTaskCountInRange(
            startMillis: Long,
            endMillis: Long,
        ): Result<Int> =
            withContext(dispatcher) {
                try {
                    Result.success(taskDao.getCompletedTaskCountInRange(startMillis, endMillis))
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override suspend fun getTotalTopLevelTaskCount(): Result<Int> =
            withContext(dispatcher) {
                try {
                    Result.success(taskDao.getTotalTopLevelTaskCount())
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
    }
