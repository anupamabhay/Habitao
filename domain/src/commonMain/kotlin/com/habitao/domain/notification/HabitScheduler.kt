package com.habitao.domain.notification

import com.habitao.domain.model.DayOfWeek
import com.habitao.domain.model.FrequencyType
import kotlinx.datetime.LocalTime

interface HabitScheduler {
    fun scheduleReminder(
        habitId: String,
        habitTitle: String,
        time: LocalTime,
        frequencyType: FrequencyType = FrequencyType.DAILY,
        scheduledDays: Set<DayOfWeek> = emptySet(),
    )

    fun cancelReminder(habitId: String)

    suspend fun rescheduleAllReminders()
}

/** No-op implementation for platforms without native scheduling (e.g. iOS). */
class NoOpHabitScheduler : HabitScheduler {
    override fun scheduleReminder(
        habitId: String,
        habitTitle: String,
        time: LocalTime,
        frequencyType: FrequencyType,
        scheduledDays: Set<DayOfWeek>,
    ) = Unit

    override fun cancelReminder(habitId: String) = Unit

    override suspend fun rescheduleAllReminders() = Unit
}
