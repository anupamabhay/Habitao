package com.habitao.data.repository

import com.habitao.data.local.dao.HabitDao
import com.habitao.data.local.dao.HabitLogDao
import com.habitao.data.local.entity.HabitLogEntity
import com.habitao.domain.model.Habit
import com.habitao.domain.model.HabitLog
import com.habitao.domain.model.StreakInfo
import com.habitao.domain.repository.HabitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepositoryImpl
    @Inject
    constructor(
        private val habitDao: HabitDao,
        private val habitLogDao: HabitLogDao,
        private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
    ) : HabitRepository {
        // ============== CRUD OPERATIONS ==============

        override suspend fun createHabit(habit: Habit): Result<Unit> =
            withContext(dispatcher) {
                try {
                    habitDao.insertHabit(habit.toEntity())
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override suspend fun updateHabit(habit: Habit): Result<Unit> =
            withContext(dispatcher) {
                try {
                    habitDao.updateHabit(habit.toEntity())
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override suspend fun deleteHabit(habitId: String): Result<Unit> =
            withContext(dispatcher) {
                try {
                    habitDao.deleteHabitById(habitId)
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override suspend fun getHabitById(habitId: String): Result<Habit> =
            withContext(dispatcher) {
                try {
                    val entity =
                        habitDao.getHabitById(habitId)
                            ?: return@withContext Result.failure(Exception("Habit not found"))
                    Result.success(entity.toDomainModel())
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override fun observeHabitById(habitId: String): Flow<Result<Habit>> {
            return habitDao.observeHabitById(habitId)
                .map { entity ->
                    entity?.let {
                        Result.success(it.toDomainModel())
                    } ?: Result.failure(Exception("Habit not found"))
                }
                .flowOn(dispatcher)
        }

        // ============== LIST OPERATIONS ==============

        override fun observeAllHabits(): Flow<Result<List<Habit>>> {
            return habitDao.observeAllHabits()
                .map { entities ->
                    Result.success(entities.map { it.toDomainModel() })
                }
                .flowOn(dispatcher)
        }

        override suspend fun getAllHabits(): Result<List<Habit>> =
            withContext(dispatcher) {
                try {
                    val entities = habitDao.getAllHabits()
                    Result.success(entities.map { it.toDomainModel() })
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override fun observeArchivedHabits(): Flow<Result<List<Habit>>> {
            return habitDao.observeArchivedHabits()
                .map { entities ->
                    Result.success(entities.map { it.toDomainModel() })
                }
                .flowOn(dispatcher)
        }

        // ============== TODAY'S HABITS ==============

        override suspend fun getHabitsForDate(date: LocalDate): Result<List<Habit>> =
            withContext(dispatcher) {
                try {
                    val dateMillis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val entities = habitDao.getHabitsForDate(dateMillis)
                    Result.success(entities.map { it.toDomainModel() }.filter { it.isScheduledFor(date) })
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override fun observeHabitsForDate(date: LocalDate): Flow<Result<List<Habit>>> {
            return habitDao.observeHabitsForDate(
                date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            )
                .map { entities ->
                    Result.success(entities.map { it.toDomainModel() }.filter { it.isScheduledFor(date) })
                }
                .catch { e -> emit(Result.failure(e)) }
                .flowOn(dispatcher)
        }

        // ============== LOG OPERATIONS ==============

        override suspend fun createOrUpdateLog(
            habitId: String,
            date: LocalDate,
            count: Int,
        ): Result<Unit> =
            withContext(dispatcher) {
                try {
                    // Get the habit to check goal
                    val habit =
                        habitDao.getHabitById(habitId)
                            ?: return@withContext Result.failure(Exception("Habit not found"))

                    // Get or create log
                    val dateMillis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val existingLog = habitLogDao.getLogForHabitAndDate(habitId, dateMillis)

                    val isCompleted = count >= habit.targetValue
                    val completedAt =
                        if (isCompleted && existingLog?.isCompleted != true) {
                            System.currentTimeMillis()
                        } else {
                            existingLog?.completedAt
                        }

                    val logEntity =
                        existingLog?.copy(
                            currentValue = count,
                            currentCount = count, // Legacy field
                            isCompleted = isCompleted,
                            completedAt = completedAt,
                            updatedAt = System.currentTimeMillis(),
                        ) ?: HabitLogEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            habitId = habitId,
                            date = dateMillis,
                            currentValue = count,
                            targetValue = habit.targetValue,
                            currentCount = count, // Legacy field
                            goalCount = habit.targetValue, // Legacy field
                            isCompleted = isCompleted,
                            completedAt = completedAt,
                        )

                    habitLogDao.insertLog(logEntity)
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override suspend fun getLogForHabitAndDate(
            habitId: String,
            date: LocalDate,
        ): Result<HabitLog?> =
            withContext(dispatcher) {
                try {
                    val dateMillis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val entity = habitLogDao.getLogForHabitAndDate(habitId, dateMillis)
                    Result.success(entity?.toDomainModel())
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override fun observeLogForHabitAndDate(
            habitId: String,
            date: LocalDate,
        ): Flow<Result<HabitLog?>> {
            val dateMillis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            return habitLogDao.observeLogForHabitAndDate(habitId, dateMillis)
                .map { entity ->
                    Result.success(entity?.toDomainModel())
                }
                .flowOn(dispatcher)
        }

        override suspend fun getLogsForHabit(habitId: String): Result<List<HabitLog>> =
            withContext(dispatcher) {
                try {
                    val entities = habitLogDao.getLogsForHabit(habitId)
                    Result.success(entities.map { it.toDomainModel() })
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override fun observeLogsForDate(date: LocalDate): Flow<Result<Map<String, HabitLog>>> {
            val dateMillis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            return habitLogDao.observeLogsForDate(dateMillis)
                .map { entities ->
                    Result.success(entities.associate { it.habitId to it.toDomainModel() })
                }
                .flowOn(dispatcher)
        }

        override fun observeLogsForDateRange(
            startDate: LocalDate,
            endDate: LocalDate,
        ): Flow<Result<List<HabitLog>>> {
            val zone = java.time.ZoneId.systemDefault()
            val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            return habitLogDao.observeLogsBetweenDates(startMillis, endMillis)
                .map { entities ->
                    Result.success(entities.map { it.toDomainModel() })
                }
                .catch { e -> emit(Result.failure(e)) }
                .flowOn(dispatcher)
        }

        override suspend fun getWeeklyProgressForHabit(
            habitId: String,
            weekContainingDate: LocalDate,
        ): Result<Int> =
            withContext(dispatcher) {
                try {
                    // Get Monday of the week containing the date (ISO 8601 week)
                    val weekStart = weekContainingDate.with(java.time.DayOfWeek.MONDAY)
                    val weekEnd = weekStart.plusDays(6)

                    val startMillis =
                        weekStart
                            .atStartOfDay(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    val endMillis =
                        weekEnd
                            .atStartOfDay(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()

                    val logs = habitLogDao.getLogsForHabitBetweenDates(habitId, startMillis, endMillis)

                    val totalProgress = logs.sumOf { it.currentValue }
                    Result.success(totalProgress)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        // ============== STATISTICS ==============

        override suspend fun calculateStreak(habitId: String): Result<StreakInfo> =
            withContext(dispatcher) {
                try {
                    // Get last 365 days of logs for streak calculation
                    val logs = habitLogDao.getCompletedLogsForStreak(habitId, 365)

                    if (logs.isEmpty()) {
                        return@withContext Result.success(StreakInfo(0, 0, 0))
                    }

                    // Calculate current streak
                    val sortedLogs = logs.sortedByDescending { it.date }
                    var currentStreak = 0
                    var previousDate =
                        java.time.Instant.ofEpochMilli(sortedLogs.first().date)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()

                    for (log in sortedLogs) {
                        val logDate =
                            java.time.Instant.ofEpochMilli(log.date)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()

                        if (logDate == previousDate || logDate == previousDate.minusDays(1)) {
                            currentStreak++
                            previousDate = logDate
                        } else {
                            break
                        }
                    }

                    // Calculate longest streak
                    var longestStreak = 0
                    var tempStreak = 0
                    var lastDate: LocalDate? = null

                    val allLogs =
                        habitLogDao.getLogsForHabit(habitId)
                            .filter { it.isCompleted }
                            .sortedBy { it.date }

                    for (log in allLogs) {
                        val logDate =
                            java.time.Instant.ofEpochMilli(log.date)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()

                        if (lastDate == null || logDate == lastDate.plusDays(1)) {
                            tempStreak++
                        } else {
                            tempStreak = 1
                        }

                        longestStreak = maxOf(longestStreak, tempStreak)
                        lastDate = logDate
                    }

                    val totalCompletions = habitLogDao.getTotalCompletions(habitId)

                    Result.success(StreakInfo(currentStreak, longestStreak, totalCompletions))
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        // ============== UTILITY ==============

        override suspend fun archiveHabit(
            habitId: String,
            archive: Boolean,
        ): Result<Unit> =
            withContext(dispatcher) {
                try {
                    habitDao.setHabitArchived(habitId, archive)
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        // ============== CHECKLIST OPERATIONS ==============

        override suspend fun toggleChecklistItem(
            habitId: String,
            date: LocalDate,
            itemId: String,
        ): Result<Unit> =
            withContext(dispatcher) {
                try {
                    val habit =
                        habitDao.getHabitById(habitId)
                            ?: return@withContext Result.failure(Exception("Habit not found"))

                    val dateMillis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val existingLog = habitLogDao.getLogForHabitAndDate(habitId, dateMillis)

                    // Parse existing completed items or start fresh
                    val currentCompleted =
                        existingLog?.completedChecklistItemsJson
                            ?.let { parseStringSet(it) }
                            ?: emptySet()

                    // Toggle the item
                    val newCompleted =
                        if (currentCompleted.contains(itemId)) {
                            currentCompleted - itemId
                        } else {
                            currentCompleted + itemId
                        }

                    val totalItems = habit.checklistJson?.let { parseChecklist(it).size } ?: 0
                    val isCompleted = newCompleted.size >= totalItems && totalItems > 0
                    val completedAt =
                        if (isCompleted && existingLog?.isCompleted != true) {
                            System.currentTimeMillis()
                        } else {
                            existingLog?.completedAt
                        }

                    val logEntity =
                        existingLog?.copy(
                            currentValue = newCompleted.size,
                            currentCount = newCompleted.size,
                            completedChecklistItemsJson = stringSetToJson(newCompleted),
                            isCompleted = isCompleted,
                            completedAt = completedAt,
                            updatedAt = System.currentTimeMillis(),
                        ) ?: HabitLogEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            habitId = habitId,
                            date = dateMillis,
                            currentValue = newCompleted.size,
                            targetValue = totalItems,
                            currentCount = newCompleted.size,
                            goalCount = totalItems,
                            completedChecklistItemsJson = stringSetToJson(newCompleted),
                            isCompleted = isCompleted,
                            completedAt = completedAt,
                        )

                    habitLogDao.insertLog(logEntity)
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        private fun parseStringSet(json: String): Set<String> {
            return try {
                val array = org.json.JSONArray(json)
                (0 until array.length()).map { array.getString(it) }.toSet()
            } catch (e: Exception) {
                emptySet()
            }
        }

        private fun stringSetToJson(set: Set<String>): String {
            val array = org.json.JSONArray()
            set.forEach { array.put(it) }
            return array.toString()
        }

        private fun parseChecklist(json: String): List<com.habitao.domain.model.ChecklistItem> {
            return try {
                val array = org.json.JSONArray(json)
                (0 until array.length()).map { index ->
                    val obj = array.getJSONObject(index)
                    com.habitao.domain.model.ChecklistItem(
                        id = obj.getString("id"),
                        text = obj.getString("text"),
                        sortOrder = obj.optInt("sortOrder", index),
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
