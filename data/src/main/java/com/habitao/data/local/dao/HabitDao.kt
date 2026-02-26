package com.habitao.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.habitao.data.local.entity.HabitEntity
import com.habitao.data.local.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow

// DAO for habit operations
@Dao
interface HabitDao {
    // ============== HABIT CRUD ==============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: String)

    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getHabitById(habitId: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE id = :habitId")
    fun observeHabitById(habitId: String): Flow<HabitEntity?>

    // ============== LIST QUERIES ==============

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY sortOrder ASC, createdAt DESC")
    fun observeAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY sortOrder ASC, createdAt DESC")
    suspend fun getAllHabits(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE isArchived = 1 ORDER BY createdAt DESC")
    fun observeArchivedHabits(): Flow<List<HabitEntity>>

    // ============== TODAY'S HABITS ==============

    @Query(
        """
        SELECT * FROM habits 
        WHERE isArchived = 0 
        AND startDate <= :date
        AND (endDate IS NULL OR endDate >= :date)
        ORDER BY sortOrder ASC, createdAt DESC
    """,
    )
    suspend fun getHabitsForDate(date: Long): List<HabitEntity>

    @Query(
        """
        SELECT * FROM habits 
        WHERE isArchived = 0 
        AND startDate <= :date
        AND (endDate IS NULL OR endDate >= :date)
        ORDER BY sortOrder ASC, createdAt DESC
    """,
    )
    fun observeHabitsForDate(date: Long): Flow<List<HabitEntity>>

    // ============== ARCHIVE ==============

    @Query("UPDATE habits SET isArchived = :isArchived WHERE id = :habitId")
    suspend fun setHabitArchived(
        habitId: String,
        isArchived: Boolean,
    )
}

// DAO for habit log operations
@Dao
interface HabitLogDao {
    // ============== LOG CRUD ==============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLogEntity): Long

    @Update
    suspend fun updateLog(log: HabitLogEntity)

    @Delete
    suspend fun deleteLog(log: HabitLogEntity)

    @Query("DELETE FROM habit_logs WHERE id = :logId")
    suspend fun deleteLogById(logId: String)

    @Query("SELECT * FROM habit_logs WHERE id = :logId")
    suspend fun getLogById(logId: String): HabitLogEntity?

    // ============== LIST QUERIES ==============

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY date DESC")
    fun observeLogsForHabit(habitId: String): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY date DESC")
    suspend fun getLogsForHabit(habitId: String): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getLogsBetweenDates(
        startDate: Long,
        endDate: Long,
    ): List<HabitLogEntity>

    @Query(
        """
        SELECT * FROM habit_logs 
        WHERE habitId = :habitId AND date BETWEEN :startDate AND :endDate 
        ORDER BY date DESC
        """,
    )
    suspend fun getLogsForHabitBetweenDates(
        habitId: String,
        startDate: Long,
        endDate: Long,
    ): List<HabitLogEntity>

    // ============== SPECIFIC DATE ==============

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun getLogForHabitAndDate(
        habitId: String,
        date: Long,
    ): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date")
    fun observeLogForHabitAndDate(
        habitId: String,
        date: Long,
    ): Flow<HabitLogEntity?>

    @Query("SELECT * FROM habit_logs WHERE date = :date")
    fun observeLogsForDate(date: Long): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE date >= :startDate AND date < :endDate ORDER BY date DESC")
    fun observeLogsBetweenDates(
        startDate: Long,
        endDate: Long,
    ): Flow<List<HabitLogEntity>>

    // ============== STREAK CALCULATION ==============

    @Query(
        """
        SELECT * FROM habit_logs 
        WHERE habitId = :habitId 
        AND isCompleted = 1 
        ORDER BY date DESC 
        LIMIT :limit
    """,
    )
    suspend fun getCompletedLogsForStreak(
        habitId: String,
        limit: Int,
    ): List<HabitLogEntity>

    @Query(
        """
        SELECT COUNT(*) FROM habit_logs 
        WHERE habitId = :habitId 
        AND isCompleted = 1
    """,
    )
    suspend fun getTotalCompletions(habitId: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM habit_logs 
        WHERE habitId = :habitId 
        AND date BETWEEN :startDate AND :endDate
        AND isCompleted = 1
    """,
    )
    suspend fun getCompletionsBetweenDates(
        habitId: String,
        startDate: Long,
        endDate: Long,
    ): Int
}
