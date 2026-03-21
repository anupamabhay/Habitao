package com.habitao.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.daysUntil

data class Routine(
    val id: String,
    val title: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val repeatPattern: RepeatPattern,
    val repeatDays: List<DayOfWeek>? = null,
    val customInterval: Int? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val nextScheduledDate: LocalDate,
    val completionThreshold: Float = 1.0f,
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime? = null,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val lastSyncedAt: Long? = null,
    val deletedAt: Long? = null,
) {
    /** Check if this routine is scheduled for the given date based on its repeat pattern. */
    fun isScheduledForDate(date: LocalDate): Boolean {
        if (date < startDate) return false
        if (endDate != null && date > endDate) return false

        return when (repeatPattern) {
            RepeatPattern.DAILY -> true
            // WEEKLY and SPECIFIC_DATES both use repeatDays to define specific days of the week.
            // SPECIFIC_DATES is named for legacy/Habits compatibility but functionally means
            // "repeat on specific weekdays" — identical scheduling logic to WEEKLY.
            RepeatPattern.WEEKLY, RepeatPattern.SPECIFIC_DATES -> {
                val days = repeatDays
                if (days.isNullOrEmpty()) true else date.dayOfWeek.toDomainDay() in days
            }
            RepeatPattern.CUSTOM -> {
                val interval = customInterval ?: 1
                val daysBetween = startDate.daysUntil(date).toLong()
                daysBetween >= 0 && daysBetween % interval == 0L
            }
        }
    }
}
