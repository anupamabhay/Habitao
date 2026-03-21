package com.habitao.domain.notification

import com.habitao.domain.model.DayOfWeek
import com.habitao.domain.model.RepeatPattern
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

interface RoutineScheduler {
    fun scheduleReminder(
        routineId: String,
        routineTitle: String,
        time: LocalTime,
        repeatPattern: RepeatPattern,
        repeatDays: Set<DayOfWeek>,
        customInterval: Int,
        startDate: LocalDate,
    )

    fun cancelReminder(routineId: String)

    suspend fun rescheduleAllReminders()
}

class NoOpRoutineScheduler : RoutineScheduler {
    override fun scheduleReminder(
        routineId: String,
        routineTitle: String,
        time: LocalTime,
        repeatPattern: RepeatPattern,
        repeatDays: Set<DayOfWeek>,
        customInterval: Int,
        startDate: LocalDate,
    ) = Unit

    override fun cancelReminder(routineId: String) = Unit

    override suspend fun rescheduleAllReminders() = Unit
}
