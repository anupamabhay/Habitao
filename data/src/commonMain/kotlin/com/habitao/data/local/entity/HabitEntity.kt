package com.habitao.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import kotlinx.datetime.Clock

/**
 * Room entity representing a habit.
 * Supports new tracking model with HabitType, flexible scheduling, and presets.
 */
@Entity(
    tableName = "habits",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["isArchived"]),
        Index(value = ["startDate"]),
    ],
)
data class HabitEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    // Tracking Configuration (new model)
    val habitType: String = HabitTypeEntity.SIMPLE.name,
    val targetValue: Int = 1,
    val unit: String? = null,
    val targetOperator: String = TargetOperatorEntity.AT_LEAST.name,
    val checklistJson: String? = null, // JSON array of ChecklistItem
    // Legacy field - kept for migration, maps to targetValue
    val goalCount: Int = 1,
    // Scheduling (new flexible model)
    val frequencyType: String = FrequencyTypeEntity.DAILY.name,
    val frequencyValue: Int = 1,
    val scheduledDaysJson: String? = null, // JSON array of DayOfWeek names
    val startDate: Long, // Epoch millis
    val endDate: Long? = null, // Epoch millis
    // Reminders
    val reminderEnabled: Boolean = false,
    val reminderTimeMinutes: Int? = null, // Minutes from midnight (0-1439)
    // Metadata
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
)

// Habit type enum for database storage
enum class HabitTypeEntity {
    SIMPLE, // Binary yes/no
    MEASURABLE, // Numeric with target
    CHECKLIST, // Multiple sub-tasks
}

// Target operator for measurable habits
enum class TargetOperatorEntity {
    AT_LEAST,
    AT_MOST,
}

// Frequency type for scheduling
enum class FrequencyTypeEntity {
    DAILY,
    SPECIFIC_DAYS,
    TIMES_PER_WEEK,
    EVERY_X_DAYS,
}

// Legacy enums for backward compatibility
@Deprecated("Use HabitTypeEntity instead")
enum class TrackingType {
    COUNT,
    DURATION,
    BINARY,
}

@Deprecated("Use FrequencyTypeEntity instead")
enum class RepeatPattern {
    DAILY,
    WEEKLY,
    CUSTOM,
    SPECIFIC_DATES,
}
