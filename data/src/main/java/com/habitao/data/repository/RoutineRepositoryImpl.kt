package com.habitao.data.repository

import com.habitao.data.local.dao.RoutineDao
import com.habitao.data.local.entity.RoutineLogEntity
import com.habitao.domain.model.Routine
import com.habitao.domain.model.RoutineLog
import com.habitao.domain.model.RoutineStep
import com.habitao.domain.repository.RoutineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineRepositoryImpl @Inject constructor(
    private val routineDao: RoutineDao,
    private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) : RoutineRepository {

    override suspend fun createRoutine(routine: Routine, steps: List<RoutineStep>): Result<Unit> =
        withContext(dispatcher) {
            try {
                routineDao.createRoutineWithSteps(
                    routine.toEntity(),
                    steps.map { it.toEntity() }
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun updateRoutine(routine: Routine, steps: List<RoutineStep>): Result<Unit> =
        withContext(dispatcher) {
            try {
                routineDao.updateRoutineWithSteps(
                    routine.toEntity(),
                    steps.map { it.toEntity() }
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun deleteRoutine(routineId: String): Result<Unit> =
        withContext(dispatcher) {
            try {
                routineDao.deleteRoutineById(routineId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getRoutineById(routineId: String): Result<Routine> =
        withContext(dispatcher) {
            try {
                val entity = routineDao.getRoutineById(routineId)
                    ?: return@withContext Result.failure(Exception("Routine not found"))
                Result.success(entity.toDomainModel())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override fun observeRoutineById(routineId: String): Flow<Result<Routine>> {
        return routineDao.observeRoutineById(routineId)
            .map { entity ->
                entity?.let {
                    Result.success(it.toDomainModel())
                } ?: Result.failure(Exception("Routine not found"))
            }
            .flowOn(dispatcher)
    }

    override fun observeAllRoutines(): Flow<Result<List<Routine>>> {
        return routineDao.observeAllRoutines()
            .map { entities ->
                Result.success(entities.map { it.toDomainModel() })
            }
            .catch { e -> emit(Result.failure(e)) }
            .flowOn(dispatcher)
    }

    override suspend fun getRoutinesForDate(date: LocalDate): Result<List<Routine>> =
        withContext(dispatcher) {
            try {
                val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val entities = routineDao.getRoutinesForDate(dateMillis)
                Result.success(entities.map { it.toDomainModel() })
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override fun observeRoutinesForDate(date: LocalDate): Flow<Result<List<Routine>>> {
        val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return routineDao.observeRoutinesForDate(dateMillis)
            .map { entities ->
                Result.success(entities.map { it.toDomainModel() })
            }
            .catch { e -> emit(Result.failure(e)) }
            .flowOn(dispatcher)
    }

    override suspend fun getRoutineSteps(routineId: String): Result<List<RoutineStep>> =
        withContext(dispatcher) {
            try {
                val entities = routineDao.getRoutineSteps(routineId)
                Result.success(entities.map { it.toDomainModel() })
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override fun observeRoutineSteps(routineId: String): Flow<Result<List<RoutineStep>>> {
        return routineDao.observeRoutineSteps(routineId)
            .map { entities ->
                Result.success(entities.map { it.toDomainModel() })
            }
            .catch { e -> emit(Result.failure(e)) }
            .flowOn(dispatcher)
    }

    override suspend fun logRoutineStep(
        routineId: String,
        stepId: String,
        date: LocalDate,
        isCompleted: Boolean
    ): Result<Unit> = withContext(dispatcher) {
        try {
            val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val existingLog = routineDao.getRoutineLog(routineId, dateMillis)
            
            val steps = routineDao.getRoutineSteps(routineId)
            val totalSteps = steps.size
            
            val completedStepIds = existingLog?.completedStepIds?.toMutableList() ?: mutableListOf()
            
            if (isCompleted && !completedStepIds.contains(stepId)) {
                completedStepIds.add(stepId)
            } else if (!isCompleted) {
                completedStepIds.remove(stepId)
            }
            
            val completionPercentage = if (totalSteps > 0) {
                completedStepIds.size.toFloat() / totalSteps.toFloat()
            } else {
                0f
            }
            
            val routine = routineDao.getRoutineById(routineId)
            val threshold = routine?.completionThreshold ?: 1.0f
            val isRoutineCompleted = completionPercentage >= threshold
            
            val newLog = RoutineLogEntity(
                id = existingLog?.id ?: UUID.randomUUID().toString(),
                routineId = routineId,
                date = date,
                completedStepIds = completedStepIds,
                totalSteps = totalSteps,
                completionPercentage = completionPercentage,
                isCompleted = isRoutineCompleted,
                createdAt = existingLog?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                completedAt = if (isRoutineCompleted && existingLog?.isCompleted != true) System.currentTimeMillis() else existingLog?.completedAt
            )
            
            routineDao.insertRoutineLog(newLog)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRoutineLog(routineId: String, date: LocalDate): Result<RoutineLog?> =
        withContext(dispatcher) {
            try {
                val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val entity = routineDao.getRoutineLog(routineId, dateMillis)
                Result.success(entity?.toDomainModel())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override fun observeRoutineLog(routineId: String, date: LocalDate): Flow<Result<RoutineLog?>> {
        val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return routineDao.observeRoutineLog(routineId, dateMillis)
            .map { entity ->
                Result.success(entity?.toDomainModel())
            }
            .catch { e -> emit(Result.failure(e)) }
            .flowOn(dispatcher)
    }

    override fun observeRoutineLogsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Result<List<RoutineLog>>> {
        val zone = ZoneId.systemDefault()
        val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return routineDao.observeRoutineLogsForDateRange(startMillis, endMillis)
            .map { entities ->
                Result.success(entities.map { it.toDomainModel() })
            }
            .catch { e -> emit(Result.failure(e)) }
            .flowOn(dispatcher)
    }

    override suspend fun getCompletedRoutinesCount(date: LocalDate): Result<Int> =
        withContext(dispatcher) {
            try {
                val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                Result.success(routineDao.getCompletedRoutinesCount(dateMillis))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getTotalRoutinesCount(): Result<Int> =
        withContext(dispatcher) {
            try {
                Result.success(routineDao.getTotalRoutinesCount())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
