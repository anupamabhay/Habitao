package com.habitao.domain.repository

import com.habitao.domain.model.Habit
import com.habitao.domain.model.HabitLog
import com.habitao.domain.model.StreakInfo
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// Repository interface for habit operations
interface HabitRepository {
    // ============== CRUD OPERATIONS ==============

    suspend fun createHabit(habit: Habit): Result<Unit>

    suspend fun updateHabit(habit: Habit): Result<Unit>

    suspend fun deleteHabit(habitId: String): Result<Unit>

    suspend fun getHabitById(habitId: String): Result<Habit>

    fun observeHabitById(habitId: String): Flow<Result<Habit>>

    // ============== LIST OPERATIONS ==============

    fun observeAllHabits(): Flow<Result<List<Habit>>>

    suspend fun getAllHabits(): Result<List<Habit>>

    fun observeArchivedHabits(): Flow<Result<List<Habit>>>

    // ============== TODAY'S HABITS ==============

    suspend fun getHabitsForDate(date: LocalDate): Result<List<Habit>>

    fun observeHabitsForDate(date: LocalDate): Flow<Result<List<Habit>>>

    // ============== LOG OPERATIONS ==============

    suspend fun createOrUpdateLog(
        habitId: String,
        date: LocalDate,
        count: Int,
    ): Result<Unit>

    suspend fun getLogForHabitAndDate(
        habitId: String,
        date: LocalDate,
    ): Result<HabitLog?>

    fun observeLogForHabitAndDate(
        habitId: String,
        date: LocalDate,
    ): Flow<Result<HabitLog?>>

    suspend fun getLogsForHabit(habitId: String): Result<List<HabitLog>>

    fun observeLogsForDate(date: LocalDate): Flow<Result<Map<String, HabitLog>>>

    suspend fun getWeeklyProgressForHabit(
        habitId: String,
        weekContainingDate: LocalDate,
    ): Result<Int>

    // ============== STATISTICS ==============

    suspend fun calculateStreak(habitId: String): Result<StreakInfo>

    // ============== UTILITY ==============

    suspend fun archiveHabit(
        habitId: String,
        archive: Boolean = true,
    ): Result<Unit>

    // ============== CHECKLIST OPERATIONS ==============

    suspend fun toggleChecklistItem(
        habitId: String,
        date: LocalDate,
        itemId: String,
    ): Result<Unit>
}
