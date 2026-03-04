package com.habitao.data.local.database

import androidx.room.TypeConverter
import com.habitao.domain.model.RepeatPattern
import com.habitao.domain.model.SyncStatus
import com.habitao.domain.model.TaskPriority
import org.json.JSONArray
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class Converters {
    // LocalDate <-> Long (epoch millis at start of day)
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? = date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    @TypeConverter
    fun toLocalDate(millis: Long?): LocalDate? =
        millis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }

    // LocalTime <-> Int (minutes from midnight)
    @TypeConverter
    fun fromLocalTime(time: LocalTime?): Int? = time?.let { it.hour * 60 + it.minute }

    @TypeConverter
    fun toLocalTime(minutes: Int?): LocalTime? = minutes?.let { LocalTime.of(it / 60, it % 60) }

    // List<DayOfWeek> <-> String (JSON)
    @TypeConverter
    fun fromDayOfWeekList(days: List<DayOfWeek>?): String? {
        if (days == null) return null
        return JSONArray(days.map { it.name }).toString()
    }

    @TypeConverter
    fun toDayOfWeekList(json: String?): List<DayOfWeek>? {
        if (json == null) return null
        val array = JSONArray(json)
        return (0 until array.length()).map { DayOfWeek.valueOf(array.getString(it)) }
    }

    // List<String> <-> String (JSON)
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        if (list == null) return null
        return JSONArray(list).toString()
    }

    @TypeConverter
    fun toStringList(json: String?): List<String>? {
        if (json == null) return null
        val array = JSONArray(json)
        return (0 until array.length()).map { array.getString(it) }
    }

    // RepeatPattern <-> String
    @TypeConverter
    fun fromRepeatPattern(pattern: RepeatPattern?): String? = pattern?.name

    @TypeConverter
    fun toRepeatPattern(name: String?): RepeatPattern? = name?.let { RepeatPattern.valueOf(it) }

    // SyncStatus <-> String
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus?): String? = status?.name

    @TypeConverter
    fun toSyncStatus(name: String?): SyncStatus? = name?.let { SyncStatus.valueOf(it) }

    // TaskPriority <-> String
    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority?): String? = priority?.name

    @TypeConverter
    fun toTaskPriority(name: String?): TaskPriority? = name?.let { TaskPriority.valueOf(it) }
}
