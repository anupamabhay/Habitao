package com.habitao.data.repository

import com.habitao.data.local.entity.FrequencyTypeEntity
import com.habitao.data.local.entity.HabitEntity
import com.habitao.data.local.entity.HabitLogEntity
import com.habitao.data.local.entity.HabitTypeEntity
import com.habitao.data.local.entity.TargetOperatorEntity
import com.habitao.domain.model.ChecklistItem
import com.habitao.domain.model.DayOfWeek
import com.habitao.domain.model.FrequencyType
import com.habitao.domain.model.Habit
import com.habitao.domain.model.HabitLog
import com.habitao.domain.model.HabitType
import com.habitao.domain.model.TargetOperator
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.json.JSONArray
import org.json.JSONObject

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
        habitType = parseHabitType(habitType),
        targetValue = targetValue,
        unit = unit,
        targetOperator = parseTargetOperator(targetOperator),
        checklist = checklistJson?.let { parseChecklist(it) } ?: emptyList(),
        frequencyType = parseFrequencyType(frequencyType),
        frequencyValue = frequencyValue,
        scheduledDays = scheduledDaysJson?.let { parseDaysSet(it) } ?: emptySet(),
        startDate = millisToLocalDate(startDate),
        endDate = endDate?.let { millisToLocalDate(it) },
        reminderEnabled = reminderEnabled,
        reminderTime = reminderTimeMinutes?.let { minutesToLocalTime(it) },
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
        habitType = habitType.name,
        targetValue = targetValue,
        unit = unit,
        targetOperator = targetOperator.name,
        checklistJson = checklist.takeIf { it.isNotEmpty() }?.let { checklistToJson(it) },
        goalCount = targetValue, // Legacy field
        frequencyType = frequencyType.name,
        frequencyValue = frequencyValue,
        scheduledDaysJson = scheduledDays.takeIf { it.isNotEmpty() }?.let { daysSetToJson(it) },
        startDate = localDateToMillis(startDate),
        endDate = endDate?.let { localDateToMillis(it) },
        reminderEnabled = reminderEnabled,
        reminderTimeMinutes = reminderTime?.let { localTimeToMinutes(it) },
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
        currentValue = currentValue,
        targetValue = targetValue,
        isCompleted = isCompleted,
        completedChecklistItems = completedChecklistItemsJson?.let { parseStringSet(it) } ?: emptySet(),
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
        currentValue = currentValue,
        targetValue = targetValue,
        isCompleted = isCompleted,
        completedChecklistItemsJson = completedChecklistItems.takeIf { it.isNotEmpty() }?.let { stringSetToJson(it) },
        currentCount = currentValue, // Legacy field
        goalCount = targetValue, // Legacy field
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt
    )
}

// ============== TYPE PARSERS ==============

private fun parseHabitType(value: String): HabitType {
    return try {
        HabitType.valueOf(value)
    } catch (e: Exception) {
        // Handle legacy TrackingType values
        when (value) {
            "COUNT", "DURATION" -> HabitType.MEASURABLE
            "BINARY" -> HabitType.SIMPLE
            else -> HabitType.SIMPLE
        }
    }
}

private fun parseTargetOperator(value: String): TargetOperator {
    return try {
        TargetOperator.valueOf(value)
    } catch (e: Exception) {
        TargetOperator.AT_LEAST
    }
}

private fun parseFrequencyType(value: String): FrequencyType {
    return try {
        FrequencyType.valueOf(value)
    } catch (e: Exception) {
        // Handle legacy RepeatPattern values
        when (value) {
            "DAILY" -> FrequencyType.DAILY
            "WEEKLY" -> FrequencyType.SPECIFIC_DAYS
            "CUSTOM" -> FrequencyType.EVERY_X_DAYS
            "SPECIFIC_DATES" -> FrequencyType.SPECIFIC_DAYS
            else -> FrequencyType.DAILY
        }
    }
}

// ============== DATE/TIME UTILITIES ==============

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

private fun minutesToLocalTime(minutes: Int): LocalTime {
    return LocalTime.of(minutes / 60, minutes % 60)
}

private fun localTimeToMinutes(time: LocalTime): Int {
    return time.hour * 60 + time.minute
}

// ============== JSON UTILITIES ==============

private fun parseDaysSet(json: String): Set<DayOfWeek> {
    return try {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { index ->
            try {
                DayOfWeek.valueOf(array.getString(index))
            } catch (e: Exception) {
                null
            }
        }.toSet()
    } catch (e: Exception) {
        emptySet()
    }
}

private fun daysSetToJson(days: Set<DayOfWeek>): String {
    val array = JSONArray()
    days.forEach { array.put(it.name) }
    return array.toString()
}

private fun parseChecklist(json: String): List<ChecklistItem> {
    return try {
        val array = JSONArray(json)
        (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            ChecklistItem(
                id = obj.getString("id"),
                text = obj.getString("text"),
                sortOrder = obj.optInt("sortOrder", index)
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun checklistToJson(items: List<ChecklistItem>): String {
    val array = JSONArray()
    items.forEach { item ->
        val obj = JSONObject().apply {
            put("id", item.id)
            put("text", item.text)
            put("sortOrder", item.sortOrder)
        }
        array.put(obj)
    }
    return array.toString()
}

private fun parseStringSet(json: String): Set<String> {
    return try {
        val array = JSONArray(json)
        (0 until array.length()).map { array.getString(it) }.toSet()
    } catch (e: Exception) {
        emptySet()
    }
}

private fun stringSetToJson(set: Set<String>): String {
    val array = JSONArray()
    set.forEach { array.put(it) }
    return array.toString()
}
