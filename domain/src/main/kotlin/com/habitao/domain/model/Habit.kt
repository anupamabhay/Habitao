package com.habitao.domain.model

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Domain model representing a habit.
 * Supports three tracking types:
 * - SIMPLE: Binary yes/no (e.g., Take medication, Make bed)
 * - MEASURABLE: Numeric with target (e.g., Drink 2000ml water, Read 30 minutes)
 * - CHECKLIST: Multiple sub-tasks (e.g., Morning routine with steps)
 */
data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    // Tracking Configuration
    val habitType: HabitType = HabitType.SIMPLE,
    val targetValue: Int = 1,
    val unit: String? = null,
    val targetOperator: TargetOperator = TargetOperator.AT_LEAST,
    val checklist: List<ChecklistItem> = emptyList(),
    // Legacy field for backward compatibility - maps to targetValue
    @Deprecated("Use targetValue instead", ReplaceWith("targetValue"))
    val goalCount: Int = targetValue,
    // Scheduling - flexible frequency support
    val frequencyType: FrequencyType = FrequencyType.DAILY,
    val frequencyValue: Int = 1,
    val scheduledDays: Set<DayOfWeek> = emptySet(),
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    // Reminders
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime? = null,
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
) {
    /**
     * Check if this habit is scheduled for a given date
     */
    fun isScheduledFor(date: LocalDate): Boolean {
        if (date < startDate) return false
        if (endDate != null && date > endDate) return false

        return when (frequencyType) {
            FrequencyType.DAILY -> true
            FrequencyType.SPECIFIC_DAYS -> scheduledDays.contains(date.dayOfWeek.toDomainDay())
            FrequencyType.TIMES_PER_WEEK -> true // Flexible - user decides when
            FrequencyType.EVERY_X_DAYS -> {
                val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
                daysSinceStart % frequencyValue == 0L
            }
        }
    }

    /**
     * Get human-readable frequency description
     */
    fun getFrequencyDescription(): String {
        return when (frequencyType) {
            FrequencyType.DAILY -> "Every day"
            FrequencyType.SPECIFIC_DAYS -> {
                if (scheduledDays.size == 7) {
                    "Every day"
                } else {
                    scheduledDays.sorted().joinToString(", ") { it.shortName }
                }
            }
            FrequencyType.TIMES_PER_WEEK -> "$frequencyValue times per week"
            FrequencyType.EVERY_X_DAYS -> "Every $frequencyValue days"
        }
    }

    /**
     * Get target description for display
     */
    fun getTargetDescription(): String {
        return when (habitType) {
            HabitType.SIMPLE -> ""
            HabitType.MEASURABLE -> {
                val op = if (targetOperator == TargetOperator.AT_MOST) "at most" else ""
                val unitStr = unit ?: "times"
                "$op $targetValue $unitStr".trim()
            }
            HabitType.CHECKLIST -> "${checklist.size} items"
        }
    }
}

/**
 * Type of habit tracking
 */
enum class HabitType {
    SIMPLE, // Binary yes/no completion
    MEASURABLE, // Numeric progress toward target
    CHECKLIST, // Multiple sub-tasks to complete
}

/**
 * For measurable habits: track "at least X" or "at most X"
 * AT_MOST is useful for breaking bad habits (e.g., "smoke at most 5 cigarettes")
 */
enum class TargetOperator {
    AT_LEAST,
    AT_MOST,
}

/**
 * Scheduling frequency type
 */
enum class FrequencyType {
    DAILY, // Every day
    SPECIFIC_DAYS, // Mon, Wed, Fri (uses scheduledDays)
    TIMES_PER_WEEK, // 3 times per week (uses frequencyValue)
    EVERY_X_DAYS, // Every 3 days (uses frequencyValue)
}

/**
 * Day of week enum with display helpers
 */
enum class DayOfWeek(val shortName: String) {
    MONDAY("Mon"),
    TUESDAY("Tue"),
    WEDNESDAY("Wed"),
    THURSDAY("Thu"),
    FRIDAY("Fri"),
    SATURDAY("Sat"),
    SUNDAY("Sun"),
}

/**
 * Extension to convert Java DayOfWeek to domain DayOfWeek
 */
fun java.time.DayOfWeek.toDomainDay(): DayOfWeek {
    return when (this) {
        java.time.DayOfWeek.MONDAY -> DayOfWeek.MONDAY
        java.time.DayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
        java.time.DayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
        java.time.DayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
        java.time.DayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
        java.time.DayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
        java.time.DayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
    }
}

/**
 * Item in a checklist habit
 */
data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val sortOrder: Int = 0,
)

/**
 * Legacy enum for backward compatibility
 */
@Deprecated("Use HabitType instead")
enum class TrackingType {
    COUNT,
    DURATION,
    BINARY,
    ;

    fun toHabitType(): HabitType =
        when (this) {
            COUNT, DURATION -> HabitType.MEASURABLE
            BINARY -> HabitType.SIMPLE
        }
}

/**
 * Legacy enum for backward compatibility
 */
@Deprecated("Use FrequencyType instead")
enum class RepeatPattern {
    DAILY,
    WEEKLY,
    CUSTOM,
    SPECIFIC_DATES,
    ;

    fun toFrequencyType(): FrequencyType =
        when (this) {
            DAILY -> FrequencyType.DAILY
            WEEKLY -> FrequencyType.SPECIFIC_DAYS
            CUSTOM -> FrequencyType.EVERY_X_DAYS
            SPECIFIC_DATES -> FrequencyType.SPECIFIC_DAYS
        }
}

/**
 * Domain model representing daily progress for a habit.
 * Tracks completion status and progress for a specific date.
 */
data class HabitLog(
    val id: String = UUID.randomUUID().toString(),
    val habitId: String,
    val date: LocalDate,
    // Progress tracking
    val currentValue: Int = 0,
    val targetValue: Int = 1,
    val isCompleted: Boolean = false,
    // Checklist progress (for CHECKLIST habits)
    val completedChecklistItems: Set<String> = emptySet(),
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
) {
    // Legacy property for backward compatibility
    @Deprecated("Use currentValue instead", ReplaceWith("currentValue"))
    val currentCount: Int get() = currentValue

    @Deprecated("Use targetValue instead", ReplaceWith("targetValue"))
    val goalCount: Int get() = targetValue

    /**
     * Calculate progress as a fraction (0.0 to 1.0)
     */
    val progress: Float
        get() =
            if (targetValue > 0) {
                (currentValue.toFloat() / targetValue).coerceIn(0f, 1f)
            } else {
                if (isCompleted) 1f else 0f
            }
}

/**
 * Information about habit streaks and statistics
 */
data class StreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletions: Int,
    val completionRate: Float = 0f, // 0.0 to 1.0
)

/**
 * Preset habit templates for quick creation
 */
data class HabitPreset(
    val title: String,
    val description: String? = null,
    val icon: String,
    val category: PresetCategory,
    val habitType: HabitType,
    val targetValue: Int = 1,
    val unit: String? = null,
    val frequencyType: FrequencyType = FrequencyType.DAILY,
    val frequencyValue: Int = 1,
)

enum class PresetCategory {
    HEALTH,
    FITNESS,
    MINDFULNESS,
    PRODUCTIVITY,
    LEARNING,
}

/**
 * Built-in habit presets
 */
object HabitPresets {
    val all =
        listOf(
            // Health
            HabitPreset(
                title = "Drink Water",
                description = "Stay hydrated throughout the day",
                icon = "water_drop",
                category = PresetCategory.HEALTH,
                habitType = HabitType.MEASURABLE,
                targetValue = 8,
                unit = "glasses",
            ),
            HabitPreset(
                title = "Take Medication",
                description = "Take daily medication on time",
                icon = "medication",
                category = PresetCategory.HEALTH,
                habitType = HabitType.SIMPLE,
            ),
            HabitPreset(
                title = "Sleep 8 Hours",
                description = "Get enough rest",
                icon = "bedtime",
                category = PresetCategory.HEALTH,
                habitType = HabitType.MEASURABLE,
                targetValue = 8,
                unit = "hours",
            ),
            // Fitness
            HabitPreset(
                title = "Workout",
                description = "Exercise at the gym",
                icon = "fitness_center",
                category = PresetCategory.FITNESS,
                habitType = HabitType.SIMPLE,
                frequencyType = FrequencyType.TIMES_PER_WEEK,
                frequencyValue = 3,
            ),
            HabitPreset(
                title = "Morning Walk",
                description = "Start the day with a walk",
                icon = "directions_walk",
                category = PresetCategory.FITNESS,
                habitType = HabitType.SIMPLE,
            ),
            HabitPreset(
                title = "Run",
                description = "Go for a run",
                icon = "directions_run",
                category = PresetCategory.FITNESS,
                habitType = HabitType.MEASURABLE,
                targetValue = 30,
                unit = "minutes",
            ),
            // Mindfulness
            HabitPreset(
                title = "Meditation",
                description = "Practice mindfulness",
                icon = "self_improvement",
                category = PresetCategory.MINDFULNESS,
                habitType = HabitType.MEASURABLE,
                targetValue = 10,
                unit = "minutes",
            ),
            HabitPreset(
                title = "Gratitude Journal",
                description = "Write things you are grateful for",
                icon = "edit_note",
                category = PresetCategory.MINDFULNESS,
                habitType = HabitType.SIMPLE,
            ),
            // Productivity
            HabitPreset(
                title = "Deep Work",
                description = "Focused work without distractions",
                icon = "psychology",
                category = PresetCategory.PRODUCTIVITY,
                habitType = HabitType.MEASURABLE,
                targetValue = 4,
                unit = "pomodoros",
            ),
            HabitPreset(
                title = "No Social Media",
                description = "Limit social media usage",
                icon = "phonelink_off",
                category = PresetCategory.PRODUCTIVITY,
                habitType = HabitType.SIMPLE,
            ),
            // Learning
            HabitPreset(
                title = "Read",
                description = "Read books or articles",
                icon = "menu_book",
                category = PresetCategory.LEARNING,
                habitType = HabitType.MEASURABLE,
                targetValue = 30,
                unit = "minutes",
            ),
            HabitPreset(
                title = "Learn Language",
                description = "Practice a new language",
                icon = "translate",
                category = PresetCategory.LEARNING,
                habitType = HabitType.MEASURABLE,
                targetValue = 15,
                unit = "minutes",
            ),
        )

    fun getByCategory(category: PresetCategory): List<HabitPreset> = all.filter { it.category == category }
}
