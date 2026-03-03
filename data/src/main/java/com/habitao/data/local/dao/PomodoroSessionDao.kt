package com.habitao.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.habitao.data.local.entity.PomodoroSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(entity: PomodoroSessionEntity)

    @Query(
        "SELECT * FROM pomodoro_sessions " +
            "WHERE startedAt >= :startOfDay AND startedAt < :endOfDay " +
            "ORDER BY startedAt DESC",
    )
    suspend fun getSessionsForDateRange(
        startOfDay: Long,
        endOfDay: Long,
    ): List<PomodoroSessionEntity>

    @Query(
        "SELECT * FROM pomodoro_sessions " +
            "WHERE startedAt >= :startOfDay AND startedAt < :endOfDay " +
            "ORDER BY startedAt DESC",
    )
    fun observeSessionsForDateRange(
        startOfDay: Long,
        endOfDay: Long,
    ): Flow<List<PomodoroSessionEntity>>

    @Query(
        "SELECT COALESCE(SUM(actualDurationSeconds), 0) " +
            "FROM pomodoro_sessions " +
            "WHERE sessionType = 'WORK' " +
            "AND startedAt >= :startOfDay AND startedAt < :endOfDay",
    )
    suspend fun getTotalFocusSeconds(
        startOfDay: Long,
        endOfDay: Long,
    ): Int
}
