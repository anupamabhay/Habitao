package com.habitao.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

/**
 * Room entity representing a habit
 * Based on data model defined in docs/02-DATA-MODEL-SCHEMA.md
 */
@Entity(
    tableName = "habits",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["isArchived"]),
        Index(value = ["nextScheduledDate"])
    ]
)
data class HabitEntity(
    @PrimaryKey
    val id: String,
    
    val title: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    
    // Tracking Configuration
    val goalCount: Int = 1,
    val unit: String? = null,
    val trackingType: String = TrackingType.COUNT.name,
    
    // Scheduling
    val repeatPattern: String = RepeatPattern.DAILY.name,
    val repeatDays: String? = null, // JSON array of DayOfWeek
    val customInterval: Int? = null,
    val startDate: Long, // Epoch millis
    val endDate: Long? = null, // Epoch millis
    val nextScheduledDate: Long, // Epoch millis
    
    // Reminders
    val reminderEnabled: Boolean = false,
    val reminderTime: Long? = null, // Epoch millis for time
    val reminderDays: String? = null, // JSON array of DayOfWeek
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val sortOrder: Int = 0
)

enum class TrackingType {
    COUNT,
    DURATION,
    BINARY
}

enum class RepeatPattern {
    DAILY,
    WEEKLY,
    CUSTOM,
    SPECIFIC_DATES
}
