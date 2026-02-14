package com.habitao.domain.model

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Domain model representing a habit
 * This is what the UI and business logic work with
 */
data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    
    // Tracking Configuration
    val goalCount: Int = 1,
    val unit: String? = null,
    val trackingType: TrackingType = TrackingType.COUNT,
    
    // Scheduling
    val repeatPattern: RepeatPattern = RepeatPattern.DAILY,
    val repeatDays: List<DayOfWeek> = emptyList(),
    val customInterval: Int? = null,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val nextScheduledDate: LocalDate = LocalDate.now(),
    
    // Reminders
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime? = null,
    val reminderDays: List<DayOfWeek>? = null,
    
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

enum class DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

/**
 * Domain model representing daily progress for a habit
 */
data class HabitLog(
    val id: String = UUID.randomUUID().toString(),
    val habitId: String,
    val date: LocalDate,
    val currentCount: Int = 0,
    val goalCount: Int,
    val isCompleted: Boolean = false,
    val durationSeconds: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/**
 * Information about habit streaks
 */
data class StreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletions: Int
)
