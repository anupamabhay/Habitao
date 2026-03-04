package com.habitao.domain.repository

import com.habitao.domain.model.PomodoroSession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface PomodoroRepository {
    suspend fun saveSession(session: PomodoroSession): Result<Unit>

    suspend fun getSessionsForDate(date: LocalDate): Result<List<PomodoroSession>>

    fun observeSessionsForDate(date: LocalDate): Flow<Result<List<PomodoroSession>>>

    fun observeSessionsForDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<Result<List<PomodoroSession>>>

    suspend fun getTotalFocusTimeForDate(date: LocalDate): Result<Int>
}
