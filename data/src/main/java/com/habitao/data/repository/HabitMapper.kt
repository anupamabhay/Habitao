package com.habitao.data.repository

import com.habitao.data.local.dao.HabitDao
import com.habitao.data.local.dao.HabitLogDao
import com.habitao.data.local.entity.HabitEntity
import com.habitao.data.local.entity.HabitLogEntity
import com.habitao.data.local.entity.RepeatPattern
import com.habitao.data.local.entity.TrackingType
import com.habitao.domain.model.DayOfWeek
import com.habitao.domain.model.Habit
import com.habitao.domain.model.HabitLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.json.JSONArray

/**
 * Extension functions to convert between Entity and Domain models
 */

// ============== HABIT MAPPERS ==============

fun HabitEntity.toDomainModel(): Habit {
    return Habit(
        id = id,
        title = title,
        description = description,
        icon = icon,
        color = color,
        goalCount = goalCount,
        unit = unit,
        trackingType = TrackingType.valueOf(trackingType),
        repeatPattern = com.habitao.domain.model.RepeatPattern.valueOf(repeatPattern),
        repeatDays = repeatDays?.let { parseDaysList(it) } ?: emptyList(),
        customInterval = customInterval,
        startDate = millisToLocalDate(startDate),
        endDate = endDate?.let { millisToLocalDate(it) },
        nextScheduledDate = millisToLocalDate(nextScheduledDate),
        reminderEnabled = reminderEnabled,
        reminderTime = reminderTime?.let { millisToLocalTime(it) },
        reminderDays = reminderDays?.let { parseDaysList(it) },
        createdAt = createdAt,
        updatedAt = updatedAt,
        isArchived = isArchived,
        sortOrder = sortOrder
    )
}

fun Habit.toEntity(): HabitEntity {
    return HabitEntity(
        id = id,
        title = title,
        description = description,
        icon = icon,
        color = color,
        goalCount = goalCount,
        unit = unit,
        trackingType = trackingType.name,
        repeatPattern = repeatPattern.name,
        repeatDays = repeatDays.takeIf { it.isNotEmpty() }?.let { daysListToJson(it) },
        customInterval = customInterval,
        startDate = localDateToMillis(startDate),
        endDate = endDate?.let { localDateToMillis(it) },
        nextScheduledDate = localDateToMillis(nextScheduledDate),
        reminderEnabled = reminderEnabled,
        reminderTime = reminderTime?.let { localTimeToMillis(it) },
        reminderDays = reminderDays?.takeIf { it.isNotEmpty() }?.let { daysListToJson(it) },
        createdAt = createdAt,
        updatedAt = updatedAt,
        isArchived = isArchived,
        sortOrder = sortOrder
    )
}

// ============== LOG MAPPERS ==============

fun HabitLogEntity.toDomainModel(): HabitLog {
    return HabitLog(
        id = id,
        habitId = habitId,
        date = millisToLocalDate(date),
        currentCount = currentCount,
        goalCount = goalCount,
        isCompleted = isCompleted,
        durationSeconds = durationSeconds,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt
    )
}

fun HabitLog.toEntity(): HabitLogEntity {
    return HabitLogEntity(
        id = id,
        habitId = habitId,
        date = localDateToMillis(date),
        currentCount = currentCount,
        goalCount = goalCount,
        isCompleted = isCompleted,
        durationSeconds = durationSeconds,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt
    )
}

// ============== UTILITIES ==============

private fun millisToLocalDate(millis: Long): LocalDate {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

private fun localDateToMillis(date: LocalDate): Long {
    return date.atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

private fun millisToLocalTime(millis: Long): LocalTime {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
}

private fun localTimeToMillis(time: LocalTime): Long {
    return time.atDate(LocalDate.now())
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

private fun parseDaysList(json: String): List<DayOfWeek> {
    return try {
        val array = JSONArray(json)
        List(array.length()) { index ->
            DayOfWeek.valueOf(array.getString(index))
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun daysListToJson(days: List<DayOfWeek>): String {
    val array = JSONArray()
    days.forEach { array.put(it.name) }
    return array.toString()
}
