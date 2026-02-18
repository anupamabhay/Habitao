package com.habitao.system.notifications

import com.habitao.domain.model.HabitType
import com.habitao.domain.repository.HabitRepository
import java.time.LocalDate
import javax.inject.Inject

class HabitCompletionService
    @Inject
    constructor(
        private val habitRepository: HabitRepository,
    ) {
        suspend fun markHabitComplete(habitId: String) {
            val habit = habitRepository.getHabitById(habitId).getOrNull() ?: return
            val date = LocalDate.now()
            val count =
                when (habit.habitType) {
                    HabitType.SIMPLE -> 1
                    HabitType.MEASURABLE -> habit.targetValue
                    HabitType.CHECKLIST -> habit.targetValue
                }
            habitRepository.createOrUpdateLog(habitId, date, count)
        }
    }
