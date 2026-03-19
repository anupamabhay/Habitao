package com.habitao.data.local.database

import androidx.room.TypeConverter
import com.habitao.domain.model.DayOfWeek
import com.habitao.domain.model.RepeatPattern
import com.habitao.domain.model.SyncStatus
import com.habitao.domain.model.TaskPriority
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonPrimitive

class Converters {
    // LocalDate <-> Long (epoch millis at start of day in system timezone)
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? =
        date?.atStartOfDayIn(TimeZone.currentSystemDefault())?.toEpochMilliseconds()

    @TypeConverter
    fun toLocalDate(millis: Long?): LocalDate? =
        millis?.let {
            Instant.fromEpochMilliseconds(it)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
        }

    // LocalTime <-> Int (minutes from midnight)
    @TypeConverter
    fun fromLocalTime(time: LocalTime?): Int? = time?.let { it.hour * 60 + it.minute }

    @TypeConverter
    fun toLocalTime(minutes: Int?): LocalTime? =
        minutes?.let { LocalTime(it / 60, it % 60) }

    // List<DayOfWeek> <-> String (JSON array of names)
    @TypeConverter
    fun fromDayOfWeekList(days: List<DayOfWeek>?): String? {
        if (days == null) return null
        return buildJsonArray { days.forEach { add(JsonPrimitive(it.name)) } }.toString()
    }

    @TypeConverter
    fun toDayOfWeekList(jsonStr: String?): List<DayOfWeek>? {
        if (jsonStr == null) return null
        return try {
            val array = Json.parseToJsonElement(jsonStr) as JsonArray
            array.map { DayOfWeek.valueOf(it.jsonPrimitive.content) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // List<String> <-> String (JSON array)
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        if (list == null) return null
        return buildJsonArray { list.forEach { add(JsonPrimitive(it)) } }.toString()
    }

    @TypeConverter
    fun toStringList(jsonStr: String?): List<String>? {
        if (jsonStr == null) return null
        return try {
            val array = Json.parseToJsonElement(jsonStr) as JsonArray
            array.map { it.jsonPrimitive.content }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // RepeatPattern <-> String
    @TypeConverter
    fun fromRepeatPattern(pattern: RepeatPattern?): String? = pattern?.name

    @TypeConverter
    fun toRepeatPattern(name: String?): RepeatPattern? =
        name?.let { runCatching { RepeatPattern.valueOf(it) }.getOrNull() }

    // SyncStatus <-> String
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus?): String? = status?.name

    @TypeConverter
    fun toSyncStatus(name: String?): SyncStatus? =
        name?.let { runCatching { SyncStatus.valueOf(it) }.getOrNull() }

    // TaskPriority <-> String
    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority?): String? = priority?.name

    @TypeConverter
    fun toTaskPriority(name: String?): TaskPriority? =
        name?.let { runCatching { TaskPriority.valueOf(it) }.getOrNull() }
}
