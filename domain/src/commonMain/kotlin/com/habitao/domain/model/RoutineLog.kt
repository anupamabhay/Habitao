package com.habitao.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

data class RoutineLog(
    val id: String,
    val routineId: String,
    val date: LocalDate,
    val completedStepIds: List<String> = emptyList(),
    val totalSteps: Int,
    val completionPercentage: Float = 0f,
    val isCompleted: Boolean = false,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val completedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val deletedAt: Long? = null,
)
