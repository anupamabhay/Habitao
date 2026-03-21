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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import com.habitao.domain.model.ChecklistItem
import com.habitao.domain.util.randomUUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonPrimitive

class HabitRepositoryImpl
    constructor(
        private val habitDao: HabitDao,
        private val habitLogDao: HabitLogDao,
        private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Default,
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
                    val dateMillis = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                    val entities = habitDao.getHabitsForDate(dateMillis)
                    Result.success(entities.map { it.toDomainModel() }.filter { it.isScheduledFor(date) })
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override fun observeHabitsForDate(date: LocalDate): Flow<Result<List<Habit>>> {
            return habitDao.observeHabitsForDate(
                date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
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
                    val dateMillis = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                    val existingLog = habitLogDao.getLogForHabitAndDate(habitId, dateMillis)

                    val isCompleted = count >= habit.targetValue
                    val completedAt =
                        if (isCompleted && existingLog?.isCompleted != true) {
                            Clock.System.now().toEpochMilliseconds()
                        } else {
                            existingLog?.completedAt
                        }

                    val logEntity =
                        existingLog?.copy(
                            currentValue = count,
                            currentCount = count, // Legacy field
                            isCompleted = isCompleted,
                            completedAt = completedAt,
                            updatedAt = Clock.System.now().toEpochMilliseconds(),
                        ) ?: HabitLogEntity(
                            id = randomUUID(),
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
                    val dateMillis = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
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
            val dateMillis = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
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
            val dateMillis = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
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
            val startMillis = startDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            val endMillis = endDate.plus(1, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
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
                    val daysFromMonday = weekContainingDate.dayOfWeek.ordinal
                    val weekStart = weekContainingDate.minus(DatePeriod(days = daysFromMonday))
                    val weekEnd = weekStart.plus(DatePeriod(days = 6))

                    val startMillis = weekStart.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                    val endMillis = weekEnd.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()

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
                    val tz = TimeZone.currentSystemDefault()
                    val today = Clock.System.now().toLocalDateTime(tz).date

                    // Get last 365 days of completed logs for streak calculation
                    val logs = habitLogDao.getCompletedLogsForStreak(habitId, 365)

                    if (logs.isEmpty()) {
                        return@withContext Result.success(StreakInfo(0, 0, 0))
                    }

                    // Calculate current streak — most recent log must be today or
                    // yesterday, otherwise there is no active streak.
                    val sortedLogs = logs.sortedByDescending { it.date }
                    val mostRecentDate =
                        Instant.fromEpochMilliseconds(sortedLogs.first().date)
                            .toLocalDateTime(tz)
                            .date

                    var currentStreak = 0
                    if (mostRecentDate >= today.minus(DatePeriod(days = 1))) {
                        var previousDate = mostRecentDate
                        for (log in sortedLogs) {
                            val logDate =
                                Instant.fromEpochMilliseconds(log.date)
                                    .toLocalDateTime(tz)
                                    .date

                            if (logDate == previousDate || logDate == previousDate.minus(DatePeriod(days = 1))) {
                                // Deduplicate same-date logs — only count the date once
                                if (logDate != previousDate || currentStreak == 0) {
                                    currentStreak++
                                }
                                previousDate = logDate
                            } else {
                                break
                            }
                        }
                    }

                    // Calculate longest streak (all-time)
                    var longestStreak = 0
                    var tempStreak = 0
                    var lastDate: LocalDate? = null

                    val allLogs =
                        habitLogDao.getLogsForHabit(habitId)
                            .filter { it.isCompleted }
                            .sortedBy { it.date }

                    for (log in allLogs) {
                        val logDate =
                            Instant.fromEpochMilliseconds(log.date)
                                .toLocalDateTime(tz)
                                .date

                        if (lastDate == null || logDate == lastDate.plus(DatePeriod(days = 1))) {
                            tempStreak++
                        } else if (logDate != lastDate) {
                            // Reset streak only if this is a genuinely different (non-adjacent) date
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

                    val dateMillis = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
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
                            Clock.System.now().toEpochMilliseconds()
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
                            updatedAt = Clock.System.now().toEpochMilliseconds(),
                        ) ?: HabitLogEntity(
                            id = randomUUID(),
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
                val array = Json.parseToJsonElement(json) as JsonArray
                array.map { it.jsonPrimitive.content }.toSet()
            } catch (e: Exception) {
                emptySet()
            }
        }

        private fun stringSetToJson(set: Set<String>): String {
            return buildJsonArray { set.forEach { add(JsonPrimitive(it)) } }.toString()
        }

        private fun parseChecklist(json: String): List<ChecklistItem> {
            return try {
                val array = Json.parseToJsonElement(json) as JsonArray
                array.map { element ->
                    val obj = element as JsonObject
                    ChecklistItem(
                        id = obj["id"]!!.jsonPrimitive.content,
                        text = obj["text"]!!.jsonPrimitive.content,
                        sortOrder = obj["sortOrder"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
