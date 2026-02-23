package com.habitao.domain.repository

import com.habitao.domain.model.Routine
import com.habitao.domain.model.RoutineLog
import com.habitao.domain.model.RoutineStep
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface RoutineRepository {
    suspend fun createRoutine(routine: Routine, steps: List<RoutineStep>): Result<Unit>
    suspend fun updateRoutine(routine: Routine, steps: List<RoutineStep>): Result<Unit>
    suspend fun deleteRoutine(routineId: String): Result<Unit>
    suspend fun getRoutineById(routineId: String): Result<Routine>
    fun observeRoutineById(routineId: String): Flow<Result<Routine>>
    fun observeAllRoutines(): Flow<Result<List<Routine>>>
    suspend fun getRoutinesForDate(date: LocalDate): Result<List<Routine>>
    fun observeRoutinesForDate(date: LocalDate): Flow<Result<List<Routine>>>
    
    suspend fun getRoutineSteps(routineId: String): Result<List<RoutineStep>>
    fun observeRoutineSteps(routineId: String): Flow<Result<List<RoutineStep>>>
    
    suspend fun logRoutineStep(routineId: String, stepId: String, date: LocalDate, isCompleted: Boolean): Result<Unit>
    suspend fun getRoutineLog(routineId: String, date: LocalDate): Result<RoutineLog?>
    fun observeRoutineLog(routineId: String, date: LocalDate): Flow<Result<RoutineLog?>>
}
