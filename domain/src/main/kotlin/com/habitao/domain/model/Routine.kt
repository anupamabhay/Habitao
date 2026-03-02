package com.habitao.domain.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class Routine(
    val id: String,
    val title: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val repeatPattern: RepeatPattern,
    val repeatDays: List<java.time.DayOfWeek>? = null,
    val customInterval: Int? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val nextScheduledDate: LocalDate,
    val completionThreshold: Float = 1.0f,
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val lastSyncedAt: Long? = null,
    val deletedAt: Long? = null,
) {
    /** Check if this routine is scheduled for the given date based on its repeat pattern. */
    fun isScheduledForDate(date: LocalDate): Boolean {
        if (date.isBefore(startDate)) return false
        if (endDate != null && date.isAfter(endDate)) return false

        return when (repeatPattern) {
            RepeatPattern.DAILY -> true
            RepeatPattern.WEEKLY, RepeatPattern.SPECIFIC_DATES -> {
                val days = repeatDays
                if (days.isNullOrEmpty()) true else date.dayOfWeek in days
            }
            RepeatPattern.CUSTOM -> {
                val interval = customInterval ?: 1
                val daysBetween = ChronoUnit.DAYS.between(startDate, date)
                daysBetween >= 0 && daysBetween % interval == 0L
            }
        }
    }
}
