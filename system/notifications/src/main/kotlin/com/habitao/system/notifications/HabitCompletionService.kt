package com.habitao.system.notifications

import com.habitao.domain.model.HabitType
import com.habitao.domain.repository.HabitRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class HabitCompletionService
    constructor(
        private val habitRepository: HabitRepository,
    ) {
        suspend fun markHabitComplete(habitId: String) {
            val habit = habitRepository.getHabitById(habitId).getOrNull() ?: return
            val date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val count =
                when (habit.habitType) {
                    HabitType.SIMPLE -> 1
                    HabitType.MEASURABLE -> habit.targetValue
                    HabitType.CHECKLIST -> habit.targetValue
                }
            habitRepository.createOrUpdateLog(habitId, date, count)
        }
    }
