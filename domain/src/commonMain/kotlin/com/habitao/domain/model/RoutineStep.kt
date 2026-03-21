package com.habitao.domain.model

import kotlinx.datetime.Clock

data class RoutineStep(
    val id: String,
    val routineId: String,
    val stepOrder: Int,
    val title: String,
    val description: String? = null,
    val estimatedDurationMinutes: Int? = null,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val deletedAt: Long? = null,
)
