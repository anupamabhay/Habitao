package com.habitao.domain.model

import java.time.LocalDate

data class RoutineLog(
    val id: String,
    val routineId: String,
    val date: LocalDate,
    val completedStepIds: List<String> = emptyList(),
    val totalSteps: Int,
    val completionPercentage: Float = 0f,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val deletedAt: Long? = null,
)
