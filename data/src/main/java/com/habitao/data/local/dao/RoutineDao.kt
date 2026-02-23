package com.habitao.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.habitao.data.local.entity.RoutineEntity
import com.habitao.data.local.entity.RoutineLogEntity
import com.habitao.data.local.entity.RoutineStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity)

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Query("DELETE FROM routines WHERE id = :routineId")
    suspend fun deleteRoutineById(routineId: String)

    @Query("SELECT * FROM routines WHERE id = :routineId")
    suspend fun getRoutineById(routineId: String): RoutineEntity?

    @Query("SELECT * FROM routines WHERE id = :routineId")
    fun observeRoutineById(routineId: String): Flow<RoutineEntity?>

    @Query("SELECT * FROM routines WHERE isArchived = 0 ORDER BY sortOrder ASC")
    fun observeAllRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE isArchived = 0 AND nextScheduledDate <= :dateMillis ORDER BY sortOrder ASC")
    suspend fun getRoutinesForDate(dateMillis: Long): List<RoutineEntity>

    @Query("SELECT * FROM routines WHERE isArchived = 0 AND nextScheduledDate <= :dateMillis ORDER BY sortOrder ASC")
    fun observeRoutinesForDate(dateMillis: Long): Flow<List<RoutineEntity>>

    // Steps
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineSteps(steps: List<RoutineStepEntity>)

    @Query("DELETE FROM routine_steps WHERE routineId = :routineId")
    suspend fun deleteRoutineSteps(routineId: String)

    @Query("SELECT * FROM routine_steps WHERE routineId = :routineId ORDER BY stepOrder ASC")
    suspend fun getRoutineSteps(routineId: String): List<RoutineStepEntity>

    @Query("SELECT * FROM routine_steps WHERE routineId = :routineId ORDER BY stepOrder ASC")
    fun observeRoutineSteps(routineId: String): Flow<List<RoutineStepEntity>>

    // Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineLog(log: RoutineLogEntity)

    @Query("SELECT * FROM routine_logs WHERE routineId = :routineId AND date = :dateMillis")
    suspend fun getRoutineLog(routineId: String, dateMillis: Long): RoutineLogEntity?

    @Query("SELECT * FROM routine_logs WHERE routineId = :routineId AND date = :dateMillis")
    fun observeRoutineLog(routineId: String, dateMillis: Long): Flow<RoutineLogEntity?>

    @Transaction
    suspend fun createRoutineWithSteps(routine: RoutineEntity, steps: List<RoutineStepEntity>) {
        insertRoutine(routine)
        insertRoutineSteps(steps)
    }

    @Transaction
    suspend fun updateRoutineWithSteps(routine: RoutineEntity, steps: List<RoutineStepEntity>) {
        updateRoutine(routine)
        deleteRoutineSteps(routine.id)
        insertRoutineSteps(steps)
    }
}
