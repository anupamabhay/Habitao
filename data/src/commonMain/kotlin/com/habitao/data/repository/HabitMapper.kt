package com.habitao.data.repository

import com.habitao.data.local.entity.HabitEntity
import com.habitao.data.local.entity.HabitLogEntity
import com.habitao.domain.model.ChecklistItem
import com.habitao.domain.model.DayOfWeek
import com.habitao.domain.model.FrequencyType
import com.habitao.domain.model.Habit
import com.habitao.domain.model.HabitLog
import com.habitao.domain.model.HabitType
import com.habitao.domain.model.TargetOperator
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

// HABIT MAPPERS

// Entity <-> Domain model mappers
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
        sortOrder = sortOrder,
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
        sortOrder = sortOrder,
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
        completedAt = completedAt,
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
        completedAt = completedAt,
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
    return Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
}

private fun localDateToMillis(date: LocalDate): Long {
    return date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}

private fun minutesToLocalTime(minutes: Int): LocalTime {
    return LocalTime(minutes / 60, minutes % 60)
}

private fun localTimeToMinutes(time: LocalTime): Int {
    return time.hour * 60 + time.minute
}

// ============== JSON UTILITIES ==============

private fun parseDaysSet(json: String): Set<DayOfWeek> {
    return try {
        val array = Json.parseToJsonElement(json) as JsonArray
        array.mapNotNull { element ->
            runCatching { DayOfWeek.valueOf(element.jsonPrimitive.content) }.getOrNull()
        }.toSet()
    } catch (e: Exception) {
        emptySet()
    }
}

private fun daysSetToJson(days: Set<DayOfWeek>): String {
    return buildJsonArray { days.forEach { add(JsonPrimitive(it.name)) } }.toString()
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

private fun checklistToJson(items: List<ChecklistItem>): String {
    return buildJsonArray {
        items.forEach { item ->
            add(
                buildJsonObject {
                    put("id", item.id)
                    put("text", item.text)
                    put("sortOrder", item.sortOrder)
                },
            )
        }
    }.toString()
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
